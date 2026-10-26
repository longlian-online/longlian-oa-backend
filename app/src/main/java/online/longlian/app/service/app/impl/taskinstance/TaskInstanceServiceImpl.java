package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.NotificationCreateBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitResultBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddResultBO;
import online.longlian.app.pojo.bo.app.TaskClaimResultBO;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAssignParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskSubmissionVersionsParamsBO;

import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;

import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.ItemTaskFlow;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;

import online.longlian.app.service.app.ProjectService;
import online.longlian.app.service.app.InboxNotificationService;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.UserOperationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskInstanceServiceImpl implements TaskInstanceService {

    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final ItemMapper itemMapper;
    private final ProjectMapper projectMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final TaskInstanceAssembler taskInstanceAssembler;
    private final TaskInstanceCommandHandler taskInstanceCommandHandler;
    private final UserOperationLogService operationLogService;
    private final ItemTaskFlowMapper itemTaskFlowMapper;
    private final OrganizationMembershipService organizationMembershipService;
    private final PlatformTransactionManager transactionManager;
    private final ProjectService projectService;
    private final InboxNotificationService inboxNotificationService;
    private final Clock clock;

    @Override
    public List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        Project project = projectMapper.selectById(item.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        List<TaskInstance> instances = taskInstanceMapper.selectList(
                new LambdaQueryWrapper<TaskInstance>()
                        .eq(TaskInstance::getItemId, params.getItemId())
                        .orderByAsc(TaskInstance::getCreatedAt));

        if (instances.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> nodeIds = instances.stream().map(TaskInstance::getItemTaskNodeId).distinct().toList();
        List<ItemTaskNode> nodes = itemTaskNodeMapper.selectBatchIds(nodeIds);
        Map<Long, ItemTaskNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(ItemTaskNode::getId, Function.identity()));

        return taskInstanceAssembler.assembleInstances(instances, nodeMap);
    }

    @Override
    public TaskClaimResultBO claimTask(TaskInstanceOperateParamsBO params) {
        return mutateTask(params.getInstanceId(), params.getOrgId(), null, (instance, project) -> {
            taskInstanceCommandHandler.claim(instance, params.getUserId());
            ProjectWorkshopAddResultBO membership = projectService.addToWorkshop(
                    ProjectWorkshopAddParamsBO.builder()
                            .projectId(instance.getProjectId())
                            .userId(params.getUserId())
                            .orgId(params.getOrgId())
                            .build());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_CLAIM, params);
            return new TaskClaimResultBO(membership.isAdded());
        });
    }

    @Override
    public TaskClaimResultBO assignTask(TaskInstanceAssignParamsBO params) {
        return mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), (instance, project) -> {
            if (!params.getUserId().equals(project.getCreatorId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可分配任务");
            }
            organizationMembershipService.requireEnabledMember(params.getAssigneeId(), params.getOrgId());
            taskInstanceCommandHandler.assign(instance, params.getAssigneeId(), params.getExpectedRevision());
            ProjectWorkshopAddResultBO membership = projectService.addToWorkshop(ProjectWorkshopAddParamsBO.builder()
                    .projectId(instance.getProjectId())
                    .userId(params.getAssigneeId())
                    .orgId(params.getOrgId())
                    .build());
            LocalDateTime assignedAt = LocalDateTime.now(clock);
            inboxNotificationService.createBatch(new NotificationBatchCreateParamsBO(project.getOrgId(),
                    List.of(new NotificationCreateBO(params.getAssigneeId(), InboxNotificationKind.TASK_ASSIGNED,
                            instance.getId(), IdWorker.getId(), assignedAt))));
            logOwnerOperation(instance, params.getUserId(), params.getExpectedRevision(), null,
                    UserOperationType.TASK_ASSIGN, "OWNER_ASSIGN", params.getAssigneeId());
            return new TaskClaimResultBO(membership.isAdded());
        });
    }

    @Override
    public void abandonTask(TaskInstanceAbandonParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), (instance, project) -> {
            boolean projectCreator = params.getUserId().equals(project.getCreatorId());
            taskInstanceCommandHandler.abandon(instance, params.getUserId(), projectCreator,
                    params.getExpectedRevision());
            if (projectCreator && !params.getUserId().equals(instance.getAssigneeId())) {
                logOwnerOperation(instance, params.getUserId(), params.getExpectedRevision(), null,
                        UserOperationType.TASK_ABANDON, "OWNER_CANCEL");
            } else {
                operationLogService.log(params.getUserId(), instance.getProjectId(),
                        instance.getItemId(), UserOperationType.TASK_ABANDON, params);
            }
            return null;
        });
    }

    @Override
    public void submitTask(TaskInstanceSubmitParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, (instance, project) -> {
            TaskInstanceSubmitResultBO result = taskInstanceCommandHandler.submit(
                    instance, params.getUserId(), params.getOrgId(), params.getValues());
            recordProgressNotifications(instance, project, result, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_SUBMIT, params);
            return null;
        });
    }

    private void recordProgressNotifications(TaskInstance instance, Project project,
                                             TaskInstanceSubmitResultBO result, Long actorUserId) {
        List<ItemStageNodeStatusBO> beforeNodes = result.getBeforeNodes();
        List<ItemStageNodeStatusBO> afterNodes = taskInstanceMapper.selectItemStageNodeStatuses(
                List.of(instance.getItemId()));
        List<Long> unlockedNodeIds = afterNodes.stream()
                .filter(node -> !StageUnlockPolicy.isUnlocked(node.getSort(), beforeNodes)
                        && StageUnlockPolicy.isUnlocked(node.getSort(), afterNodes))
                .map(ItemStageNodeStatusBO::getNodeId).toList();
        List<NotificationCreateBO> notifications = new ArrayList<>();
        if (!unlockedNodeIds.isEmpty()) {
            List<TaskInstance> targets = taskInstanceMapper.selectList(new LambdaQueryWrapper<TaskInstance>()
                    .eq(TaskInstance::getItemId, instance.getItemId())
                    .in(TaskInstance::getItemTaskNodeId, unlockedNodeIds)
                    .eq(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                    .isNotNull(TaskInstance::getAssigneeId));
            for (TaskInstance target : targets) {
                if (java.util.Objects.equals(target.getAssigneeId(), actorUserId)) {
                    continue;
                }
                notifications.add(new NotificationCreateBO(target.getAssigneeId(), InboxNotificationKind.TASK_READY,
                        target.getId(), result.getSubmissionId(), result.getTriggeredAt()));
            }
        }
        if (!afterNodes.isEmpty() && afterNodes.stream().allMatch(node -> Boolean.TRUE.equals(node.getCompleted()))
                && !beforeNodes.stream().allMatch(node -> Boolean.TRUE.equals(node.getCompleted()))
                && !java.util.Objects.equals(project.getCreatorId(), actorUserId)) {
            notifications.add(new NotificationCreateBO(project.getCreatorId(), InboxNotificationKind.ITEM_COMPLETED,
                    instance.getItemId(), result.getSubmissionId(), result.getTriggeredAt()));
        }
        inboxNotificationService.createBatch(new NotificationBatchCreateParamsBO(project.getOrgId(), notifications));
    }

    @Override
    public void resetTask(TaskInstanceOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, (instance, project) -> {
            taskInstanceCommandHandler.reset(instance, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_RESET, params);
            return null;
        });
    }

    @Override
    public void rejectTask(TaskInstanceRejectParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), (instance, project) -> {
            boolean projectCreator = params.getUserId().equals(project.getCreatorId());
            taskInstanceCommandHandler.reject(instance, params.getUserId(), projectCreator,
                    params.getExpectedRevision(), params.getReviewComment());
            if (projectCreator) {
                logOwnerOperation(instance, params.getUserId(), params.getExpectedRevision(), params.getReviewComment(),
                        UserOperationType.TASK_REJECT, "OWNER_REJECT");
            } else {
                operationLogService.log(params.getUserId(), instance.getProjectId(),
                        instance.getItemId(), UserOperationType.TASK_REJECT, params);
            }
            return null;
        });
    }

    private void logOwnerOperation(TaskInstance instance, Long userId, String expectedRevision, String reviewComment,
                                   UserOperationType type, String action) {
        logOwnerOperation(instance, userId, expectedRevision, reviewComment, type, action, null);
    }

    private void logOwnerOperation(TaskInstance instance, Long userId, String expectedRevision, String reviewComment,
                                   UserOperationType type, String action, Long assignedUserId) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("action", action);
        audit.put("instanceId", instance.getId());
        audit.put("assigneeId", assignedUserId != null ? assignedUserId : instance.getAssigneeId());
        if (reviewComment != null) {
            audit.put("reviewComment", reviewComment);
        }
        audit.put("expectedRevision", expectedRevision);
        audit.put("revision", Long.toString(instance.getRevision() + 1));
        operationLogService.logSynchronously(userId, instance.getProjectId(),
                instance.getItemId(), type, audit);
    }

    /**
     * 每个 item 的任务流唯一；先锁任务流行，避免阻塞项目本身的普通字段更新。
     * 锁随事务结束释放；排队期间目标版本发生变化的请求不作用于新一轮接取或提交。
     */
    private <T> T mutateTask(Long instanceId, Long orgId, Long validatedUserId,
                             BiFunction<TaskInstance, Project, T> mutation) {
        TaskInstance locator = taskInstanceMapper.selectById(instanceId);
        if (locator == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> {
            ItemTaskFlow flow = itemTaskFlowMapper.selectOne(new LambdaQueryWrapper<ItemTaskFlow>()
                    .eq(ItemTaskFlow::getItemId, locator.getItemId())
                    .last("FOR UPDATE"));
            if (flow == null) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
            }
            TaskInstance instance = taskInstanceMapper.selectById(instanceId);
            if (instance == null) {
                throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
            }
            if (!java.util.Objects.equals(locator.getItemId(), instance.getItemId())
                    || !java.util.Objects.equals(locator.getRevision(), instance.getRevision())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "任务状态或归属已变更，请刷新后重试");
            }
            Project project = getAndValidateProject(instance, flow, orgId, validatedUserId);
            return mutation.apply(instance, project);
        });
    }

    @Override
    public TaskInstanceDetailVO getTaskInstanceDetail(TaskInstanceDetailParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        Item item = itemMapper.selectById(instance.getItemId());
        Project project = item != null ? projectMapper.selectById(item.getProjectId()) : null;
        if (project == null || !project.getOrgId().equals(params.getOrgId())
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        TaskSubmission submission = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, params.getInstanceId())
                        .eq(TaskSubmission::getStatus, online.longlian.common.enumeration.TaskSubmissionStatus.SUBMITTED)
                        .orderByDesc(TaskSubmission::getCreatedAt)
                        .last("LIMIT 1"));

        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务节点不存在");
        }
        return taskInstanceAssembler.assembleDetail(instance, node, submission, params.getOrgId());
    }

    @Override
    public List<TaskSubmissionVersionVO> listSubmissionVersions(TaskSubmissionVersionsParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        Item item = itemMapper.selectById(instance.getItemId());
        Project project = item != null ? projectMapper.selectById(item.getProjectId()) : null;
        if (project == null || !project.getOrgId().equals(params.getOrgId())
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务节点不存在");
        }

        List<TaskSubmission> submissions = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, params.getInstanceId())
                        .orderByDesc(TaskSubmission::getVersionNo));

        return taskInstanceAssembler.assembleSubmissionVersions(instance, node, submissions, params.getOrgId());
    }

    private Project getAndValidateProject(TaskInstance instance, ItemTaskFlow flow, Long orgId, Long validatedUserId) {
        Project project = projectMapper.selectById(instance.getProjectId());
        if (project == null || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (!project.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该任务");
        }
        Item item = itemMapper.selectById(instance.getItemId());
        if (item == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }
        if (item.getDeletedAt() != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "项目已删除，不可操作任务");
        }
        if (validatedUserId != null) {
            organizationMembershipService.requireEnabledMember(validatedUserId, orgId);
            if (instance.getDeletedAt() != null || project.getDeletedAt() != null
                    || !instance.getProjectId().equals(item.getProjectId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
            }
            requireTaskStructure(instance, flow);
        }
        return project;
    }

    private void requireTaskStructure(TaskInstance instance, ItemTaskFlow flow) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null || node.getDeletedAt() != null
                || !instance.getItemId().equals(node.getItemId())
                || !instance.getProjectId().equals(node.getProjectId())
                || !instance.getTaskFlowId().equals(node.getItemTaskFlowId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
        }
        if (flow.getDeletedAt() != null || !instance.getTaskFlowId().equals(flow.getId())
                || !instance.getItemId().equals(flow.getItemId())
                || !instance.getProjectId().equals(flow.getProjectId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
        }
    }
}

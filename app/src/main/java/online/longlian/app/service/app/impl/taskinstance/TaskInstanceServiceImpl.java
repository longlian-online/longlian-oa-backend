package online.longlian.app.service.app.impl.taskinstance;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.TaskDraftConstants;
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
import online.longlian.app.pojo.bo.app.TaskInstanceRejectResultBO;
import online.longlian.app.pojo.bo.app.ProjectAttachmentDownloadResultBO;
import online.longlian.app.pojo.bo.app.TaskAttachmentDownloadParamsBO;
import online.longlian.app.pojo.bo.app.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceProxySubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftContentResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSubmitResultBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileBindParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileListParamsBO;
import online.longlian.app.pojo.bo.common.TaskFormField;
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
import online.longlian.app.service.app.impl.taskdraft.TaskDraftRevisionPolicy;
import online.longlian.app.service.resource.TaskDraftResourceService;
import online.longlian.common.enumeration.InboxNotificationKind;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.app.service.app.TaskDraftRecordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.UserOperationType;
import online.longlian.common.enumeration.TaskDraftStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class TaskInstanceServiceImpl implements TaskInstanceService {

    private static final DateTimeFormatter SUBMITTED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
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
    private final TaskDraftRecordService taskDraftRecordService;
    private final TaskDraftResourceService taskDraftResourceService;
    private final TaskFormService taskFormService;
    private final ResourceService resourceService;

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
            if (!managesPlanning(project, params.getUserId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可分配任务");
            }
            organizationMembershipService.requireAssignableMember(params.getAssigneeId(), params.getOrgId());
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
            boolean projectCreator = managesPlanning(project, params.getUserId());
            taskInstanceCommandHandler.abandon(instance, params.getUserId(), projectCreator,
                    params.getExpectedRevision());
            taskDraftRecordService.discardTaskDrafts(
                    new TaskDraftDiscardParamsBO(List.of(instance.getId()), params.getOrgId()));
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
            TaskDraftRecordResultBO draft = taskDraftRecordService.getTaskDraftRecord(
                    new TaskDraftRecordGetParamsBO(instance.getId(), params.getOrgId()));
            if (draft != null && draft.getStatus() == TaskDraftStatus.ACTIVE) {
                throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
            }
            TaskInstanceSubmitResultBO result = taskInstanceCommandHandler.submit(
                    instance, params.getUserId(), params.getOrgId(), params.getValues());
            finishSubmission(instance, project, result, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_SUBMIT, params);
            return null;
        });
    }

    @Override
    public void proxySubmitTask(TaskInstanceProxySubmitParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), (instance, project) -> {
            if (!managesPlanning(project, params.getUserId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可代提交");
            }
            organizationMembershipService.requireAssignableMember(params.getAssigneeId(), params.getOrgId());
            TaskInstanceSubmitResultBO result = taskInstanceCommandHandler.proxySubmit(
                    instance, params.getAssigneeId(), params.getOrgId(), params.getUserId(),
                    params.getValues(), params.getSubmittedAt(), params.getExpectedRevision());
            finishSubmission(instance, project, result, params.getAssigneeId());
            Map<String, Object> audit = new LinkedHashMap<>();
            audit.put("action", "PROXY_SUBMIT");
            audit.put("instanceId", instance.getId());
            audit.put("assigneeId", params.getAssigneeId());
            audit.put("submittedAt", params.getSubmittedAt().format(SUBMITTED_AT_FORMAT));
            audit.put("expectedRevision", params.getExpectedRevision());
            audit.put("revision", Long.toString(instance.getRevision() + 1));
            operationLogService.logSynchronously(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_SUBMIT, audit);
            return null;
        });
    }

    private void finishSubmission(TaskInstance instance, Project project,
                                  TaskInstanceSubmitResultBO result, Long submitterId) {
        recordProgressNotifications(instance, project, result, submitterId);
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
            boolean projectCreator = managesPlanning(project, params.getUserId());
            TaskInstanceRejectResultBO result = taskInstanceCommandHandler.reject(instance, params.getUserId(),
                    projectCreator, params.getExpectedRevision(), params.getReviewComment());
            if (instance.getAssigneeId() != null) {
                inboxNotificationService.createBatch(new NotificationBatchCreateParamsBO(project.getOrgId(),
                        List.of(new NotificationCreateBO(instance.getAssigneeId(), InboxNotificationKind.TASK_REJECTED,
                                result.getSubmissionId(), result.getSubmissionId(), result.getTriggeredAt()))));
            }
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

    /** 创建者，或本组织启用中的组织所有者、组织管理员。不改变企划创建者。 */
    private boolean managesPlanning(Project project, Long userId) {
        return Objects.equals(userId, project.getCreatorId())
                || organizationMembershipService.isEnabledAdministrator(userId, project.getOrgId());
    }

    private TaskInstance requireInstance(Long instanceId) {
        TaskInstance instance = taskInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        return instance;
    }

    private <T> T mutateTask(Long instanceId, Long orgId, Long validatedUserId,
                             BiFunction<TaskInstance, Project, T> mutation) {
        TaskInstance locator = requireInstance(instanceId);
        return withLockedTask(locator, orgId, validatedUserId, (instance, project) -> {
            if (!Objects.equals(locator.getRevision(), instance.getRevision())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "任务状态或归属已变更，请刷新后重试");
            }
            return mutation.apply(instance, project);
        });
    }

    /** 先锁任务流行，所有任务写入共用同一把锁；存储复制在进入此事务前完成。 */
    private <T> T withLockedTask(TaskInstance locator, Long orgId, Long validatedUserId,
                                 BiFunction<TaskInstance, Project, T> mutation) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return transaction.execute(status -> {
            ItemTaskFlow flow = itemTaskFlowMapper.selectOne(new LambdaQueryWrapper<ItemTaskFlow>()
                    .eq(ItemTaskFlow::getItemId, locator.getItemId())
                    .last("FOR UPDATE"));
            if (flow == null) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
            }
            TaskInstance instance = requireInstance(locator.getId());
            if (!Objects.equals(locator.getItemId(), instance.getItemId())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "任务状态或归属已变更，请刷新后重试");
            }
            Project project = getAndValidateProject(instance, flow, orgId, validatedUserId);
            return mutation.apply(instance, project);
        });
    }

    @Override
    public TaskDraftResultBO getTaskDraft(TaskDraftGetParamsBO params) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setReadOnly(true);
        return transaction.execute(status -> {
            TaskInstance instance = requireInstance(params.getInstanceId());
            ItemTaskFlow flow = itemTaskFlowMapper.selectById(instance.getTaskFlowId());
            if (flow == null) {
                throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
            }
            getAndValidateProject(instance, flow, params.getOrgId(), params.getUserId());
            requireDraftOwner(instance, params.getUserId());
            return assembleTaskDraftResult(instance, params.getUserId(), params.getOrgId());
        });
    }

    @Override
    public TaskDraftResultBO saveTaskDraft(TaskDraftSaveParamsBO params) {
        TaskDraftResultBO initial = getTaskDraft(new TaskDraftGetParamsBO(
                params.getInstanceId(), params.getUserId(), params.getOrgId()));
        if (!initial.getTaskRevision().equals(params.getExpectedTaskRevision())
                || !initial.getDraftRevision().equals(params.getExpectedDraftRevision())) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
        TaskFormService.ValidatedValues validated = taskFormService.validateDraftValues(
                initial.getSubmitFields(), params.getValues());
        return mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(),
                (instance, project) -> persistTaskDraft(instance, params));
    }

    private TaskDraftResultBO persistTaskDraft(TaskInstance instance, TaskDraftSaveParamsBO params) {
        requireDraftOwner(instance, params.getUserId());
        if (!instance.getRevision().toString().equals(params.getExpectedTaskRevision())) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        List<TaskFormField> fields = taskFormService.parseFields(node.getMetaSchema());
        TaskFormService.ValidatedValues validated = taskFormService.validateDraftValues(fields, params.getValues());
        TaskDraftRecordResultBO current = taskDraftRecordService.getTaskDraftRecord(
                new TaskDraftRecordGetParamsBO(instance.getId(), params.getOrgId()));
        Long previousDraftId = current != null && current.getStatus() == TaskDraftStatus.ACTIVE
                ? current.getId() : null;
        TaskDraftRecordResultBO draft = taskDraftRecordService.saveTaskDraftRecord(TaskDraftRecordSaveParamsBO.builder()
                .instanceId(params.getInstanceId())
                .userId(params.getUserId())
                .orgId(params.getOrgId())
                .expectedTaskRevision(params.getExpectedTaskRevision())
                .expectedDraftRevision(params.getExpectedDraftRevision())
                .metadata(JSON.toJSONString(validated.values()))
                .build());
        resourceService.bindTaskDraftFiles(TaskDraftFileBindParamsBO.builder()
                .userId(params.getUserId())
                .orgId(params.getOrgId())
                .draftId(draft.getId())
                .previousDraftId(previousDraftId)
                .fileIds(validated.resourceIds())
                .build());
        return assembleTaskDraftResult(instance, params.getUserId(), params.getOrgId());
    }

    @Override
    public TaskDraftSubmitResultBO submitTaskDraft(TaskDraftSubmitParamsBO params) {
        // 已消费草稿的重试需先检查提交凭据，不能被普通写操作的旧任务修订号检查提前拦截。
        return withLockedTask(requireInstance(params.getInstanceId()), params.getOrgId(), params.getUserId(),
                (instance, project) -> completeTaskDraft(instance, project, params));
    }

    private TaskDraftSubmitResultBO completeTaskDraft(TaskInstance instance, Project project,
                                                      TaskDraftSubmitParamsBO params) {
        TaskDraftRecordResultBO draft = taskDraftRecordService.getTaskDraftRecord(
                new TaskDraftRecordGetParamsBO(instance.getId(), params.getOrgId()));
        TaskDraftRevisionPolicy.requireMatchingRevision(draft, params.getExpectedDraftRevision());
        TaskDraftSubmitResultBO previousResult = findPreviousDraftSubmission(instance, draft, params);
        if (previousResult != null) {
            return previousResult;
        }
        requireDraftOwner(instance, params.getUserId());
        if (!instance.getRevision().toString().equals(params.getExpectedTaskRevision())
                || !isActiveDraft(draft, instance, params.getUserId())) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
        TaskInstanceSubmitResultBO result = taskInstanceCommandHandler.submitFromDraft(instance,
                params.getUserId(), params.getOrgId(), draft.getId(), JSON.parseObject(draft.getMetadata()));
        taskDraftRecordService.consumeTaskDraftRecord(new TaskDraftConsumeParamsBO(
                draft.getId(), draft.getRevision(), result.getSubmissionId(), instance.getRevision()));
        finishSubmission(instance, project, result, params.getUserId());
        operationLogService.log(params.getUserId(), instance.getProjectId(), instance.getItemId(),
                UserOperationType.TASK_SUBMIT, Map.of("draftId", draft.getId().toString(),
                        "draftRevision", draft.getRevision().toString()));
        return new TaskDraftSubmitResultBO(instance.getId().toString(),
                Long.toString(instance.getRevision() + 1), result.getSubmissionId().toString(), "COMPLETED");
    }

    private TaskDraftSubmitResultBO findPreviousDraftSubmission(TaskInstance instance,
                                                               TaskDraftRecordResultBO draft,
                                                               TaskDraftSubmitParamsBO params) {
        if (draft == null || draft.getStatus() != TaskDraftStatus.CONSUMED
                || !Objects.equals(draft.getOwnerId(), params.getUserId())
                || !draft.getTaskRevision().toString().equals(params.getExpectedTaskRevision())
                || instance.getRevision() != Math.addExact(draft.getTaskRevision(), 1)
                || instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            return null;
        }
        TaskSubmission submission = taskSubmissionMapper.selectById(draft.getSubmittedSubmissionId());
        if (submission == null || submission.getStatus() != TaskSubmissionStatus.SUBMITTED) {
            return null;
        }
        return new TaskDraftSubmitResultBO(instance.getId().toString(), instance.getRevision().toString(),
                submission.getId().toString(), "COMPLETED");
    }

    private TaskDraftResultBO assembleTaskDraftResult(TaskInstance instance, Long userId, Long orgId) {
        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        List<TaskFormField> fields = taskFormService.parseFields(node.getMetaSchema());
        TaskDraftRecordResultBO draft = taskDraftRecordService.getTaskDraftRecord(
                new TaskDraftRecordGetParamsBO(instance.getId(), orgId));
        List<ItemStageNodeStatusBO> stages = taskInstanceMapper.selectItemStageNodeStatuses(List.of(instance.getItemId()));
        TaskDraftContentResultBO content = null;
        if (isActiveDraft(draft, instance, userId)) {
            TaskFormService.ValidatedValues validated = taskFormService.validateDraftValues(
                    fields, JSON.parseObject(draft.getMetadata()));
            content = new TaskDraftContentResultBO(draft.getId().toString(), draft.getRevision().toString(),
                    draft.getUpdatedAt(), validated.values(), taskDraftResourceService.getTaskDraftFiles(
                            new TaskDraftFileListParamsBO(orgId, draft.getId(), validated.resourceIds())));
        }
        return new TaskDraftResultBO(instance.getId().toString(), instance.getItemId().toString(),
                instance.getRevision().toString(), StageUnlockPolicy.isUnlocked(node.getSort(), stages),
                fields, draft == null ? "0" : draft.getRevision().toString(), content);
    }

    private boolean isActiveDraft(TaskDraftRecordResultBO draft, TaskInstance instance, Long userId) {
        // 任务修订号只拦截旧请求；改名、排序不使内容失效，接取结束时由写操作显式废弃草稿。
        return draft != null && draft.getStatus() == TaskDraftStatus.ACTIVE
                && Objects.equals(draft.getTaskInstanceId(), instance.getId())
                && Objects.equals(draft.getOwnerId(), userId);
    }

    private void requireDraftOwner(TaskInstance instance, Long userId) {
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED || !Objects.equals(instance.getAssigneeId(), userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅接取人可编辑任务草稿");
        }
    }

    @Override
    public TaskInstanceDetailVO getTaskInstanceDetail(TaskInstanceDetailParamsBO params) {
        TaskInstance instance = requireVisibleInstance(params.getInstanceId(), params.getOrgId());

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
    public ProjectAttachmentDownloadResultBO getTaskAttachmentDownload(TaskAttachmentDownloadParamsBO params) {
        TaskInstance instance = requireVisibleInstance(params.getInstanceId(), params.getOrgId());
        return taskInstanceAssembler.downloadAttachment(instance.getId(), params.getOrgId(), params.getFileId());
    }

    @Override
    public List<TaskSubmissionVersionVO> listSubmissionVersions(TaskSubmissionVersionsParamsBO params) {
        TaskInstance instance = requireVisibleInstance(params.getInstanceId(), params.getOrgId());

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

    private TaskInstance requireVisibleInstance(Long instanceId, Long orgId) {
        TaskInstance instance = taskInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        Item item = itemMapper.selectById(instance.getItemId());
        Project project = item != null ? projectMapper.selectById(item.getProjectId()) : null;
        if (project == null || !project.getOrgId().equals(orgId)
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        return instance;
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

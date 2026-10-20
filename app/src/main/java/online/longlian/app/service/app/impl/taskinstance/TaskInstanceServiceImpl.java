package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ItemTaskFlowMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOwnerOperateParamsBO;
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

import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.service.DistributedLockService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.UserOperationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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
    private final LockService lockService;
    private final PlatformTransactionManager transactionManager;

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
    public void claimTask(TaskInstanceOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, instance -> {
            taskInstanceCommandHandler.claim(instance, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_CLAIM, params);
        });
    }

    @Override
    public void abandonTask(TaskInstanceOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, instance -> {
            taskInstanceCommandHandler.abandon(instance, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_ABANDON, params);
        });
    }

    @Override
    public void submitTask(TaskInstanceSubmitParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, instance -> {
            taskInstanceCommandHandler.submit(instance, params.getUserId(), params.getOrgId(), params.getValues());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_SUBMIT, params);
        });
    }

    @Override
    public void resetTask(TaskInstanceOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, instance -> {
            taskInstanceCommandHandler.reset(instance, params.getUserId());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_RESET, params);
        });
    }

    @Override
    public void rejectTask(TaskInstanceRejectParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), null, instance -> {
            taskInstanceCommandHandler.reject(instance, params.getUserId(), params.getReviewComment());
            operationLogService.log(params.getUserId(), instance.getProjectId(),
                    instance.getItemId(), UserOperationType.TASK_REJECT, params);
        });
    }

    @Override
    public void ownerCancelTask(TaskInstanceOwnerOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), instance -> {
            taskInstanceCommandHandler.ownerCancel(instance, params.getExpectedRevision());
            logOwnerOperation(instance, params, UserOperationType.TASK_ABANDON, "OWNER_CANCEL");
        });
    }

    @Override
    public void ownerRejectTask(TaskInstanceOwnerOperateParamsBO params) {
        mutateTask(params.getInstanceId(), params.getOrgId(), params.getUserId(), instance -> {
            taskInstanceCommandHandler.ownerReject(instance, params.getUserId(),
                    params.getExpectedRevision(), params.getReason());
            logOwnerOperation(instance, params, UserOperationType.TASK_REJECT, "OWNER_REJECT");
        });
    }

    private void logOwnerOperation(TaskInstance instance, TaskInstanceOwnerOperateParamsBO params,
                                   UserOperationType type, String action) {
        Map<String, Object> audit = new LinkedHashMap<>();
        audit.put("action", action);
        audit.put("instanceId", instance.getId());
        audit.put("assigneeId", instance.getAssigneeId());
        audit.put("reason", params.getReason());
        audit.put("expectedRevision", params.getExpectedRevision());
        audit.put("revision", Long.toString(instance.getRevision() + 1));
        operationLogService.logSynchronously(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), type, audit);
    }

    /**
     * 定位读取只决定锁键；锁内新事务重新读取状态，且提交完成后才释放 watchdog 锁。
     * 同 item 的不同实例也必须串行，避免上游打回与下游提交分别通过前置检查。
     */
    private void mutateTask(Long instanceId, Long orgId, Long requiredCreatorId,
                            Consumer<TaskInstance> mutation) {
        TaskInstance locator = taskInstanceMapper.selectById(instanceId);
        if (locator == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(
                "task:item:" + locator.getItemId(), 0, TimeUnit.SECONDS)) {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.executeWithoutResult(status -> {
                TaskInstance instance = getAndValidateInstance(instanceId, orgId, requiredCreatorId);
                if (!java.util.Objects.equals(locator.getItemId(), instance.getItemId())) {
                    throw new AppException(ResultCode.OPERATION_FAIL, "任务归属已变更，请刷新后重试");
                }
                mutation.accept(instance);
            });
        }
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

    private TaskInstance getAndValidateInstance(Long instanceId, Long orgId, Long requiredCreatorId) {
        TaskInstance instance = taskInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
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
        if (requiredCreatorId != null) {
            organizationMembershipService.requireEnabledMember(requiredCreatorId, orgId);
            if (!requiredCreatorId.equals(project.getCreatorId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人可操作该任务");
            }
            ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
            ItemTaskFlow flow = itemTaskFlowMapper.selectById(instance.getTaskFlowId());
            if (instance.getDeletedAt() != null || project.getDeletedAt() != null
                    || !instance.getProjectId().equals(item.getProjectId())
                    || node == null || node.getDeletedAt() != null
                    || !instance.getItemId().equals(node.getItemId())
                    || !instance.getProjectId().equals(node.getProjectId())
                    || !instance.getTaskFlowId().equals(node.getItemTaskFlowId())
                    || flow == null || flow.getDeletedAt() != null
                    || !instance.getItemId().equals(flow.getItemId())
                    || !instance.getProjectId().equals(flow.getProjectId())) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "任务归属不一致，不可操作");
            }
        }
        return instance;
    }
}

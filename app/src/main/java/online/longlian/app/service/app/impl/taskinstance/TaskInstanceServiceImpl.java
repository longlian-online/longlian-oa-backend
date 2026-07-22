package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.service.app.TaskInstanceService;
import online.longlian.app.service.app.impl.UserOperationLogService;
import online.longlian.common.enumeration.UserOperationType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    @Override
    public List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null || item.getDeletedAt() != null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        Project project = projectMapper.selectById(item.getProjectId());
        if (project == null || !project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "项目不存在");
        }

        List<TaskInstance> instances = taskInstanceMapper.selectList(
                new LambdaQueryWrapper<TaskInstance>()
                        .eq(TaskInstance::getItemId, params.getItemId())
                        .isNull(TaskInstance::getDeletedAt)
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
    @Transactional(rollbackFor = Exception.class)
    public void claimTask(TaskInstanceOperateParamsBO params) {
        TaskInstance instance = getAndValidateInstance(params.getInstanceId(), params.getOrgId());
        taskInstanceCommandHandler.claim(instance, params.getUserId());
        operationLogService.log(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), UserOperationType.TASK_CLAIM, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonTask(TaskInstanceOperateParamsBO params) {
        TaskInstance instance = getAndValidateInstance(params.getInstanceId(), params.getOrgId());
        taskInstanceCommandHandler.abandon(instance, params.getUserId());
        operationLogService.log(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), UserOperationType.TASK_ABANDON, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(TaskInstanceSubmitParamsBO params) {
        TaskInstance instance = getAndValidateInstance(params.getInstanceId(), params.getOrgId());
        taskInstanceCommandHandler.submit(instance, params.getUserId(), params.getMetadata());
        operationLogService.log(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), UserOperationType.TASK_SUBMIT, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTask(TaskInstanceOperateParamsBO params) {
        TaskInstance instance = getAndValidateInstance(params.getInstanceId(), params.getOrgId());
        taskInstanceCommandHandler.reset(instance, params.getUserId());
        operationLogService.log(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), UserOperationType.TASK_RESET, params);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(TaskInstanceRejectParamsBO params) {
        TaskInstance instance = getAndValidateInstance(params.getInstanceId(), params.getOrgId());
        taskInstanceCommandHandler.reject(instance, params.getUserId(), params.getReviewComment());
        operationLogService.log(params.getUserId(), instance.getProjectId(),
                instance.getItemId(), UserOperationType.TASK_REJECT, params);
    }

    @Override
    public TaskInstanceDetailVO getTaskInstanceDetail(TaskInstanceDetailParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        Item item = itemMapper.selectById(instance.getItemId());
        Project project = item != null ? projectMapper.selectById(item.getProjectId()) : null;
        if (project == null || !project.getOrgId().equals(params.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }

        TaskSubmission submission = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, params.getInstanceId())
                        .orderByDesc(TaskSubmission::getCreatedAt)
                        .last("LIMIT 1"));

        TaskInstanceDetailVO taskInstanceDetailVO = new TaskInstanceDetailVO();
        if (submission != null) {
            taskInstanceDetailVO.setMetadata(submission.getMetadata());
        }
        return taskInstanceDetailVO;
    }

    private TaskInstance getAndValidateInstance(Long instanceId, Long orgId) {
        TaskInstance instance = taskInstanceMapper.selectById(instanceId);
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        Project project = projectMapper.selectById(instance.getProjectId());
        if (project == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (!project.getOrgId().equals(orgId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该任务");
        }
        return instance;
    }
}

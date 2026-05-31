package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
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
    private final MemberSubmitCountHandler memberSubmitCountHandler;
    private final Clock clock;

    @Override
    public List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params) {
        Item item = itemMapper.selectById(params.getItemId());
        if (item == null) {
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
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (instance.getStatus() != TaskInstanceStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可接取");
        }

        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, params.getInstanceId())
                        .set(TaskInstance::getAssigneeId, params.getUserId())
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getUpdatedAt, LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void abandonTask(TaskInstanceOperateParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可放弃");
        }
        if (!params.getUserId().equals(instance.getAssigneeId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅接取人可放弃任务");
        }

        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, params.getInstanceId())
                        .set(TaskInstance::getAssigneeId, null)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .set(TaskInstance::getUpdatedAt, LocalDateTime.now(clock)));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitTask(TaskInstanceSubmitParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可提交");
        }
        if (!params.getUserId().equals(instance.getAssigneeId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅接取人可提交任务");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, params.getInstanceId())
                        .set(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED)
                        .set(TaskInstance::getSubmittedAt, now)
                        .set(TaskInstance::getCompletedAt, now)
                        .set(TaskInstance::getUpdatedAt, now));

        TaskSubmission submission = TaskSubmission.builder()
                .projectId(instance.getProjectId())
                .itemId(instance.getItemId())
                .taskInstanceId(params.getInstanceId())
                .itemTaskNodeId(instance.getItemTaskNodeId())
                .submitterId(params.getUserId())
                .metadata(params.getMetadata())
                .status(TaskSubmissionStatus.SUBMITTED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskSubmissionMapper.insert(submission);

        memberSubmitCountHandler.incrementSubmitCount(params.getUserId(), instance.getProjectId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTask(TaskInstanceOperateParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可重置");
        }
        if (!params.getUserId().equals(instance.getAssigneeId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅接取人可重置任务");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, params.getInstanceId())
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getSubmittedAt, null)
                        .set(TaskInstance::getCompletedAt, null)
                        .set(TaskInstance::getUpdatedAt, now));

        taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, params.getInstanceId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                        .set(TaskSubmission::getStatus, TaskSubmissionStatus.RESET)
                        .set(TaskSubmission::getUpdatedAt, now));

        memberSubmitCountHandler.revertSubmitCount(params.getUserId(), instance.getProjectId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rejectTask(TaskInstanceRejectParamsBO params) {
        TaskInstance instance = taskInstanceMapper.selectById(params.getInstanceId());
        if (instance == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务实例不存在");
        }
        if (instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可打回");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, params.getInstanceId())
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getSubmittedAt, null)
                        .set(TaskInstance::getCompletedAt, null)
                        .set(TaskInstance::getUpdatedAt, now));

        taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, params.getInstanceId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                        .set(TaskSubmission::getStatus, TaskSubmissionStatus.REJECTED)
                        .set(TaskSubmission::getReviewerId, params.getUserId())
                        .set(TaskSubmission::getReviewedAt, now)
                        .set(TaskSubmission::getReviewComment, params.getReviewComment())
                        .set(TaskSubmission::getUpdatedAt, now));

        Long submitterId = instance.getAssigneeId();
        if (submitterId != null) {
            memberSubmitCountHandler.revertSubmitCount(submitterId, instance.getProjectId());
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

}

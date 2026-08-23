package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 任务实例状态流转的处理器。
 * <p>
 * 承接接取/放弃/提交/重置/打回操作中的：状态前置校验、操作权限校验、乐观锁条件更新
 */
@Component
@RequiredArgsConstructor
public class TaskInstanceCommandHandler {

    private final TaskInstanceMapper taskInstanceMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;
    private final ItemTaskNodeMapper itemTaskNodeMapper;
    private final MemberSubmitCountHandler memberSubmitCountHandler;
    private final Clock clock;

    /**
     * 接取任务：PENDING -> CLAIMED，归属操作人。
     */
    public void claim(TaskInstance instance, Long userId) {
        if (instance.getStatus() != TaskInstanceStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可接取");
        }

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .set(TaskInstance::getAssigneeId, userId)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getUpdatedAt, LocalDateTime.now(clock)));
        requireUpdated(updated);
    }

    /**
     * 放弃任务：CLAIMED -> PENDING，清空接取人。
     */
    public void abandon(TaskInstance instance, Long userId) {
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可放弃");
        }
        requireAssignee(instance, userId, "仅接取人可放弃任务");

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .eq(TaskInstance::getAssigneeId, userId)
                        .set(TaskInstance::getAssigneeId, null)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .set(TaskInstance::getUpdatedAt, LocalDateTime.now(clock)));
        requireUpdated(updated);
    }

    /**
     * 提交任务：CLAIMED -> COMPLETED，生成提交记录并累加成员提交计数。
     */
    public void submit(TaskInstance instance, Long userId, String metadata) {
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可提交");
        }
        requireAssignee(instance, userId, "仅接取人可提交任务");

        LocalDateTime now = LocalDateTime.now(clock);

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .eq(TaskInstance::getAssigneeId, userId)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED)
                        .set(TaskInstance::getSubmittedAt, now)
                        .set(TaskInstance::getCompletedAt, now)
                        .set(TaskInstance::getUpdatedAt, now));
        requireUpdated(updated);

        TaskSubmission submission = TaskSubmission.builder()
                .projectId(instance.getProjectId())
                .itemId(instance.getItemId())
                .taskInstanceId(instance.getId())
                .itemTaskNodeId(instance.getItemTaskNodeId())
                .submitterId(userId)
                .metadata(metadata)
                .status(TaskSubmissionStatus.SUBMITTED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        taskSubmissionMapper.insert(submission);

        memberSubmitCountHandler.incrementSubmitCount(userId, instance.getProjectId());
    }

    /**
     * 重置任务：COMPLETED -> CLAIMED，提交记录标记 RESET 并回退成员提交计数。
     */
    public void reset(TaskInstance instance, Long userId) {
        if (instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可重置");
        }
        requireAssignee(instance, userId, "仅接取人可重置任务");

        LocalDateTime now = LocalDateTime.now(clock);

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED)
                        .eq(TaskInstance::getAssigneeId, userId)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getSubmittedAt, null)
                        .set(TaskInstance::getCompletedAt, null)
                        .set(TaskInstance::getUpdatedAt, now));
        requireUpdated(updated);

        taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, instance.getId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                        .set(TaskSubmission::getStatus, TaskSubmissionStatus.RESET)
                        .set(TaskSubmission::getUpdatedAt, now));

        memberSubmitCountHandler.revertSubmitCount(userId, instance.getProjectId());
    }

    /**
     * 打回任务：COMPLETED -> CLAIMED，提交记录标记 REJECTED 并回退提交人计数。
     */
    public void reject(TaskInstance instance, Long userId, String reviewComment) {
        if (instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可打回");
        }
        if (userId.equals(instance.getAssigneeId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能打回自己的任务");
        }
        if (!isNextStageExecutor(instance, userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅下一阶段执行人可打回该任务");
        }

        LocalDateTime now = LocalDateTime.now(clock);

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getSubmittedAt, null)
                        .set(TaskInstance::getCompletedAt, null)
                        .set(TaskInstance::getUpdatedAt, now));
        requireUpdated(updated);

        int submissionUpdated = taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, instance.getId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                        .set(TaskSubmission::getStatus, TaskSubmissionStatus.REJECTED)
                        .set(TaskSubmission::getReviewerId, userId)
                        .set(TaskSubmission::getReviewedAt, now)
                        .set(TaskSubmission::getReviewComment, reviewComment)
                        .set(TaskSubmission::getUpdatedAt, now));
        requireUpdated(submissionUpdated);

        Long submitterId = instance.getAssigneeId();
        if (submitterId != null) {
            memberSubmitCountHandler.revertSubmitCount(submitterId, instance.getProjectId());
        }
    }

    private void requireAssignee(TaskInstance instance, Long userId, String message) {
        if (!userId.equals(instance.getAssigneeId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, message);
        }
    }

    private void requireUpdated(int updated) {
        if (updated == 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务状态已变更，请刷新后重试");
        }
    }

    /**
     * 判断 userId 是否为「下一阶段执行人」
     */
    private boolean isNextStageExecutor(TaskInstance instance, Long userId) {
        ItemTaskNode currentNode = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (currentNode == null) {
            return false;
        }

        List<ItemTaskNode> laterNodes = itemTaskNodeMapper.selectList(
                new LambdaQueryWrapper<ItemTaskNode>()
                        .eq(ItemTaskNode::getItemTaskFlowId, instance.getTaskFlowId())
                        .gt(ItemTaskNode::getSort, currentNode.getSort())
                        .orderByAsc(ItemTaskNode::getSort));
        if (laterNodes.isEmpty()) {
            return false;
        }

        int nextStageSort = laterNodes.get(0).getSort();
        List<Long> nextStageNodeIds = laterNodes.stream()
                .filter(node -> node.getSort() == nextStageSort)
                .map(ItemTaskNode::getId)
                .toList();

        Long claimedCount = taskInstanceMapper.selectCount(
                new LambdaQueryWrapper<TaskInstance>()
                        .in(TaskInstance::getItemTaskNodeId, nextStageNodeIds)
                        .eq(TaskInstance::getAssigneeId, userId));
        return claimedCount != null && claimedCount > 0;
    }
}

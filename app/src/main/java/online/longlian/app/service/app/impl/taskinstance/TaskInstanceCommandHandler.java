package online.longlian.app.service.app.impl.taskinstance;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemTaskNodeMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.entity.ItemTaskNode;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.bo.common.ResourceBindBatchParamsBO;
import online.longlian.app.service.common.TaskFormService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskInstanceStatus;
import online.longlian.common.enumeration.TaskSubmissionStatus;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

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
    private final TaskFormService taskFormService;
    private final ResourceService resourceService;

    /**
     * 接取任务：PENDING -> CLAIMED，归属操作人。
     */
    public void claim(TaskInstance instance, Long userId) {
        if (instance.getStatus() != TaskInstanceStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可接取");
        }
        LocalDateTime now = LocalDateTime.now(clock);

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getRevision, instance.getRevision())
                        .setSql("revision = revision + 1")
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .set(TaskInstance::getAssigneeId, userId)
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getClaimedAt, now)
                        .set(TaskInstance::getUpdatedAt, now));
        requireUpdated(updated);
    }


    /**
     * 版本号取实例内已有最大版本号 +1；首次提交为 1。
     * 并发由状态机乐观锁和 {@code uk_task_submission_version} 唯一键共同约束。
     */
    private Integer nextVersionNo(Long taskInstanceId) {
        TaskSubmission last = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                        .select(TaskSubmission::getVersionNo)
                        .eq(TaskSubmission::getTaskInstanceId, taskInstanceId)
                        .orderByDesc(TaskSubmission::getVersionNo)
                        .last("LIMIT 1"));
        return last == null || last.getVersionNo() == null ? 1 : last.getVersionNo() + 1;
    }

    /**
     * 提交任务：CLAIMED -> COMPLETED，生成提交记录并累加成员提交计数。
     * 前置阶段未完成时在绑定附件和改状态之前拒绝。
     */
    public void submit(TaskInstance instance, Long userId, Long orgId, Map<String, Object> values) {
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可提交");
        }
        requireAssignee(instance, userId, "仅接取人可提交任务");

        ItemTaskNode node = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (node == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务节点不存在");
        }
        requirePreviousStagesCompleted(instance, node);
        TaskFormService.ValidatedValues validated = taskFormService.validateValues(
                taskFormService.parseFields(node.getMetaSchema()), values);
        resourceService.bindBizResources(ResourceBindBatchParamsBO.builder()
                .resourceIds(validated.resourceIds())
                .bizType("task_submit")
                .bizId(instance.getId())
                .orgId(orgId)
                .creatorId(userId)
                .build());

        LocalDateTime now = LocalDateTime.now(clock);

        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getRevision, instance.getRevision())
                        .setSql("revision = revision + 1")
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
                .versionNo(nextVersionNo(instance.getId()))
                .metadata(validated.serialize())
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
                        .eq(TaskInstance::getRevision, instance.getRevision())
                        .setSql("revision = revision + 1")
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


    /** 只取消目标槽位，不删除历史提交，也不改变企划成员关系。 */
    public void abandon(TaskInstance instance, Long userId, boolean projectCreator,
                        String expectedRevision, String reason) {
        requireRevision(instance, expectedRevision);
        if (instance.getStatus() != TaskInstanceStatus.CLAIMED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可放弃");
        }
        if (!userId.equals(instance.getAssigneeId())) {
            if (!projectCreator) {
                throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅接取人或企划负责人可放弃任务");
            }
            if (reason == null || reason.isBlank() || reason.length() > 500) {
                throw new AppException(ResultCode.PARAM_ERROR, "取消他人接取须填写 1 到 500 个字符的原因");
            }
        }
        requireNoCompletedLaterStage(instance);
        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getRevision, instance.getRevision())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .setSql("revision = revision + 1")
                        .set(TaskInstance::getStatus, TaskInstanceStatus.PENDING)
                        .set(TaskInstance::getAssigneeId, null)
                        .set(TaskInstance::getClaimedAt, null)
                        .set(TaskInstance::getUpdatedAt, LocalDateTime.now(clock)));
        requireUpdated(updated);
    }

    /** 当前有效提交精确打回一次；保留原接取人、接取时间及所有成果历史。 */
    public void reject(TaskInstance instance, Long userId, boolean projectCreator,
                       String expectedRevision, String reviewComment) {
        requireRevision(instance, expectedRevision);
        if (instance.getStatus() != TaskInstanceStatus.COMPLETED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务不可打回");
        }
        if (!projectCreator && !isNextStageExecutor(instance, userId)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "仅企划负责人或下一阶段执行人可打回该任务");
        }
        requireNoCompletedLaterStage(instance);
        List<TaskSubmission> currentSubmissions = taskSubmissionMapper.selectList(
                new LambdaQueryWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getTaskInstanceId, instance.getId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED));
        if (currentSubmissions.size() != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前提交状态异常，请刷新后重试");
        }
        TaskSubmission currentSubmission = currentSubmissions.get(0);
        LocalDateTime now = LocalDateTime.now(clock);
        int updated = taskInstanceMapper.update(null,
                new LambdaUpdateWrapper<TaskInstance>()
                        .eq(TaskInstance::getId, instance.getId())
                        .eq(TaskInstance::getRevision, instance.getRevision())
                        .eq(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED)
                        .setSql("revision = revision + 1")
                        .set(TaskInstance::getStatus, TaskInstanceStatus.CLAIMED)
                        .set(TaskInstance::getSubmittedAt, null)
                        .set(TaskInstance::getCompletedAt, null)
                        .set(TaskInstance::getUpdatedAt, now));
        requireUpdated(updated);

        int submissionUpdated = taskSubmissionMapper.update(null,
                new LambdaUpdateWrapper<TaskSubmission>()
                        .eq(TaskSubmission::getId, currentSubmission.getId())
                        .eq(TaskSubmission::getTaskInstanceId, instance.getId())
                        .eq(TaskSubmission::getStatus, TaskSubmissionStatus.SUBMITTED)
                        .set(TaskSubmission::getStatus, TaskSubmissionStatus.REJECTED)
                        .set(TaskSubmission::getReviewerId, userId)
                        .set(TaskSubmission::getReviewedAt, now)
                        .set(TaskSubmission::getReviewComment, reviewComment)
                        .set(TaskSubmission::getUpdatedAt, now));
        if (submissionUpdated != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前提交状态已变更，请刷新后重试");
        }
        memberSubmitCountHandler.revertSubmitCount(currentSubmission.getSubmitterId(), instance.getProjectId());
    }

    private void requireRevision(TaskInstance instance, String expectedRevision) {
        if (!instance.getRevision().toString().equals(expectedRevision)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该任务状态已变更，请刷新后重试");
        }
    }

    private void requireNoCompletedLaterStage(TaskInstance instance) {
        ItemTaskNode currentNode = itemTaskNodeMapper.selectById(instance.getItemTaskNodeId());
        if (currentNode == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务节点不存在");
        }
        // 同 item 可有多个任务流，禁止遗漏较后 sort；同 sort 的其他并行槽不受影响。
        List<ItemTaskNode> laterNodes = itemTaskNodeMapper.selectList(
                new LambdaQueryWrapper<ItemTaskNode>()
                        .select(ItemTaskNode::getId)
                        .eq(ItemTaskNode::getItemId, instance.getItemId())
                        .gt(ItemTaskNode::getSort, currentNode.getSort()));
        if (laterNodes.isEmpty()) {
            return;
        }
        Long completed = taskInstanceMapper.selectCount(new LambdaQueryWrapper<TaskInstance>()
                .eq(TaskInstance::getItemId, instance.getItemId())
                .in(TaskInstance::getItemTaskNodeId, laterNodes.stream().map(ItemTaskNode::getId).toList())
                .eq(TaskInstance::getStatus, TaskInstanceStatus.COMPLETED));
        if (completed != null && completed > 0) {
            throw new AppException(ResultCode.OPERATION_FAIL, "后续阶段已有完成任务，不可放弃或打回");
        }
    }

    /**
     * 同一项目中 sort 更小的节点必须全部完成；同一阶段的其他并行槽不拦截。
     * 接取和放弃不调用：后续阶段可以提前接取，前置未完成只拒绝提交。
     */
    private void requirePreviousStagesCompleted(TaskInstance instance, ItemTaskNode node) {
        List<ItemStageNodeStatusBO> itemNodes = taskInstanceMapper.selectItemStageNodeStatuses(
                List.of(instance.getItemId()));
        if (!StageUnlockPolicy.isUnlocked(node.getSort(), itemNodes)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "请先完成前置阶段");
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

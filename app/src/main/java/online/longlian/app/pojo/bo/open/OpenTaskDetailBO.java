package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面任务实例详情：站内详情的提交字段组装复用，另补项目/企划标识、槽位与版本数；
 * 不含暂存（开放面无代提交场景）。
 */
public record OpenTaskDetailBO(Long instanceId, String name, Integer stage,
                               Integer slotIndex, Integer slotTotal, Integer serialStep,
                               Long itemId, String itemTitle, Long projectId, String projectTitle,
                               TaskInstanceStatus status, Long revision,
                               OpenTaskParticipantBO assignee,
                               SubmissionBO submission, long submissionCount) {

    /** 当前生效的提交；state 为 submitted / not_submitted，与站内口径一致。 */
    public record SubmissionBO(String state, LocalDateTime submittedAt, List<OpenSubmissionFieldBO> values) {
    }
}

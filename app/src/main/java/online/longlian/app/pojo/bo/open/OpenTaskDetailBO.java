package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.TaskInstanceStatus;

/**
 * 开放面任务实例详情：站内详情的提交字段组装复用，另补项目/企划标识、槽位与版本数；
 * 不含暂存（开放面无代提交场景）。
 */
public record OpenTaskDetailBO(Long instanceId, String name, Integer stage,
                               Integer slotIndex, Integer slotTotal, Integer serialStep,
                               Long itemId, String itemTitle, Long projectId, String projectTitle,
                               TaskInstanceStatus status, Long revision,
                               OpenTaskParticipantBO assignee,
                               OpenTaskSubmissionBO submission, long submissionCount) {
}

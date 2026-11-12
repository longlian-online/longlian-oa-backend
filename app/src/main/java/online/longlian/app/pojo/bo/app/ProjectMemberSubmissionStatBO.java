package online.longlian.app.pojo.bo.app;

import java.time.LocalDateTime;

/** 一名企划成员在本企划的有效提交。 */
public record ProjectMemberSubmissionStatBO(LocalDateTime lastSubmittedAt, int completedCount) {
}

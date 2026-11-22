package online.longlian.app.pojo.bo.app.project;

import java.time.LocalDateTime;

/**
 * 企划成员键集分页条目：不含头像；joinedAt 为加入企划（工坊关联）的时间。
 */
public record ProjectMemberKeysetItemBO(Long userId, String nickname, LocalDateTime joinedAt,
                                       boolean creator, int completedCount, LocalDateTime lastSubmittedAt) {
}

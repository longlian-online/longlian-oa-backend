package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;

/**
 * 开放面企划成员条目：不含头像读取链接。
 */
public record OpenProjectMemberBO(Long userId, String nickname, LocalDateTime joinedAt,
                                  boolean creator, int completedCount, LocalDateTime lastSubmittedAt) {
}

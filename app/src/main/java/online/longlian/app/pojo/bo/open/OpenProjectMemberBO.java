package online.longlian.app.pojo.bo.open;

import online.longlian.app.pojo.bo.app.project.ProjectMemberKeysetItemBO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 开放面企划成员条目：不含头像读取链接。
 */
public record OpenProjectMemberBO(Long userId, String nickname, LocalDateTime joinedAt,
                                  boolean creator, int completedCount, LocalDateTime lastSubmittedAt) {

    public static OpenProjectMemberBO from(ProjectMemberKeysetItemBO item) {
        return new OpenProjectMemberBO(item.userId(), item.nickname(), item.joinedAt(),
                item.creator(), item.completedCount(), item.lastSubmittedAt());
    }

    public static List<OpenProjectMemberBO> from(List<ProjectMemberKeysetItemBO> items) {
        return items.stream().map(OpenProjectMemberBO::from).toList();
    }
}

package online.longlian.app.pojo.vo.app;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProjectMemberVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isCreator;
    private LocalDateTime lastSubmittedAt;
    private Integer completedCount;
}

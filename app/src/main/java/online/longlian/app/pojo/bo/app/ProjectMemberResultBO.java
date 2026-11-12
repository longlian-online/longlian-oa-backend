package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ProjectMemberResultBO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isCreator;
    private LocalDateTime lastSubmittedAt;
    private Integer completedCount;
}

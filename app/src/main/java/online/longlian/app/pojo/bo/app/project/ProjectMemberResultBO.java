package online.longlian.app.pojo.bo.app.project;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectMemberResultBO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isCreator;
}

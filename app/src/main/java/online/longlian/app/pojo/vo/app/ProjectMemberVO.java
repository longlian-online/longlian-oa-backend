package online.longlian.app.pojo.vo.app;

import lombok.Data;

@Data
public class ProjectMemberVO {
    private Long userId;
    private String nickname;
    private String avatarUrl;
    private Boolean isCreator;
}

package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Schema(description = "项目成员")
public class ProjectMemberVO {

    @JsonLongIdString
    @Schema(type = "string", description = "成员用户ID")
    private Long userId;

    @Schema(description = "成员昵称")
    private String nickname;

    @Schema(description = "成员头像URL")
    private String avatarUrl;

    @Schema(description = "是否企划负责人")
    private Boolean isCreator;
    private LocalDateTime lastSubmittedAt;
    private Integer completedCount;
}

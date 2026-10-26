package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@AllArgsConstructor
@Schema(description = "可分配任务的启用组员")
public class AssignableMemberVO {
    @JsonLongIdString
    @Schema(type = "string", description = "用户ID")
    private Long userId;

    @Schema(description = "昵称")
    private String nickname;

    @Schema(description = "用户名")
    private String username;
}

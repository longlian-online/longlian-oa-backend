package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@Schema(description = "登录返回信息")
public class LoginVO {

    @Schema(type = "string", description = "用户id")
    private Long userId;

    @Schema(type = "string", description = "当前组织ID")
    private Long currentOrgId;

    @Schema(description = "用户认证token")
    private String token;

    @Schema(description = "当前组织内用户角色列表")
    private List<String> roles;
}

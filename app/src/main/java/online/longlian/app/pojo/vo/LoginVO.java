package online.longlian.app.pojo.vo;

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

    @Schema(description = "用户id")
    private Long userId;

    @Schema(description = "默认组织ID")
    private Long defaultOrgId;

    @Schema(description = "用户认证token")
    private String token;

    @Schema(description = "用户角色列表")
    private List<String> roles;
}

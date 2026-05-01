package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "管理员登录结果视图对象")
public class AdminLoginVO {

    @Schema(description = "管理员ID")
    private Long adminId;

    @Schema(description = "用户名")
    private String username;

    @Schema(description = "角色")
    private String role;

    @Schema(description = "登录令牌")
    private String token;
}

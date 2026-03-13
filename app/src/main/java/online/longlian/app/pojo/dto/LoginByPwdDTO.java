package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "密码登录请求参数")
public class LoginByPwdDTO {

    @Schema(description = "用户名",example = "username")
    private String username;

    @Schema(description = "密码",example = "123456")
    private String password;
}

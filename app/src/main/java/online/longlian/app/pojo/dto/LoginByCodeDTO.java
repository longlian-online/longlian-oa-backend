package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "验证码登录请求参数")
public class LoginByCodeDTO {

    @Schema(description = "邮箱",example = "test@longlian.com")
    private String email;

    @Schema(description = "验证码",example = "123456")
    private String code;
}

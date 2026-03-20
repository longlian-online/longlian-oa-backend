package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户注册请求参数")
public class RegisterDTO {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20位之间")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过20")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$", message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "验证码不能为空")
    @Schema(description = "验证码", example = "123456", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long code;
}
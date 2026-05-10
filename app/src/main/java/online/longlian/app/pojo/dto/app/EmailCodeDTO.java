package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.common.enumeration.EmailVerifyBusinessType;

@Data
@Schema(description = "发送邮箱验证码请求参数")
public class EmailCodeDTO {
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = PatternConstants.EMAIL_PATTERN, message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "test@longlian.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotNull(message = "业务类型不能为空")
    @Schema(description = "业务类型 LOGIN-登录 REGISTER-注册 FORGOT_PASSWORD-忘记密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private EmailVerifyBusinessType businessType;
}

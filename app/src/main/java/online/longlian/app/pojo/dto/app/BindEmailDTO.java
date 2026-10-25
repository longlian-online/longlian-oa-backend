package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import online.longlian.app.common.constants.PatternConstants;

@Data
@Schema(description = "绑定邮箱请求参数")
public class BindEmailDTO {

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = PatternConstants.EMAIL_PATTERN, message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "bind@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Pattern(regexp = PatternConstants.EMAIL_CODE_PATTERN, message = "邮箱验证码必须是6位字母或数字")
    @Schema(description = "6位字母或数字邮箱验证码", example = "A1B2C3", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;
}

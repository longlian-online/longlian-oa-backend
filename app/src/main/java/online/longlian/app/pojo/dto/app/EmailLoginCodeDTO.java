package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import online.longlian.app.common.constants.PatternConstants;

@Data
@Schema(description = "验证码登录请求参数")
public class EmailLoginCodeDTO {
    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = PatternConstants.EMAIL_PATTERN, message = "邮箱格式不正确")
    @Schema(description = "邮箱", example = "test@longlian.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
}

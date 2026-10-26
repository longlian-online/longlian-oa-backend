package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.constants.PatternConstants;

@Data
@Schema(description = "修改密码请求参数")
public class ChangePasswordDTO {

    @NotBlank(message = "原密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @Schema(description = "原密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @Pattern(regexp = PatternConstants.PASSWORD_COMPLEXITY_PATTERN, message = PatternConstants.PASSWORD_COMPLEXITY_MESSAGE)
    @Schema(description = "新密码，6-20位，须同时包含大写字母、小写字母和数字", requiredMode = Schema.RequiredMode.REQUIRED)
    private String newPassword;
}

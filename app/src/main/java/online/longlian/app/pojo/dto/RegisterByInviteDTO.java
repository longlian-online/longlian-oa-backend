package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.constants.PatternConstants;

@Data
@Schema(description = "通过邀请链接注册")
public class RegisterByInviteDTO {

    @NotBlank(message = "邀请token不能为空")
    @Schema(description = "邀请链接中的 token", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteToken;

    @NotBlank(message = "用户名不能为空")
    @Size(min = 4, max = 20, message = "用户名长度必须在4-20位之间")
    @Schema(description = "用户名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 20, message = "密码长度必须在6-20位之间")
    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @NotBlank(message = "确认密码不能为空")
    @Size(min = 6, max = 20, message = "确认密码长度必须在6-20位之间")
    @Schema(description = "确认密码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String confirmPassword;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 20, message = "昵称长度不能超过20")
    @Schema(description = "昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @NotBlank(message = "邮箱不能为空")
    @Pattern(regexp = PatternConstants.EMAIL_PATTERN, message = "邮箱格式不正确")
    @Schema(description = "邮箱", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

    @NotBlank(message = "邮箱验证码不能为空")
    @Schema(description = "邮箱验证码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String code;

    @Schema(description = "组织名称。仅当邀请类型为 SUPER_ADMIN_CREATE_ORG 时必填")
    private String orgName;
}

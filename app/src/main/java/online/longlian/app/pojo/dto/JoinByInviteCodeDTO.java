package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "通过邀请码加入组织请求参数")
public class JoinByInviteCodeDTO {

    @NotBlank(message = "邀请码不能为空")
    @Schema(description = "管理员生成的邀请码", requiredMode = Schema.RequiredMode.REQUIRED)
    private String inviteCode;
}

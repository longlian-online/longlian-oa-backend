package online.longlian.app.pojo.dto.app;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "更新当前用户信息请求参数")
public class UpdateMyInfoDTO {

    @NotBlank(message = "用户昵称不能为空")
    @Size(max = 20, message = "用户昵称不能超过 20 个字符")
    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nickname;

    @JsonLongIdString
    @Schema(type = "string", description = "用户头像文件ID")
    private Long avatarFileId;
}

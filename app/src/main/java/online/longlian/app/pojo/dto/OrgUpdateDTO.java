package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "修改组织信息请求参数")
public class OrgUpdateDTO {
    @NotBlank(message = "组织名称不能为空")
    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "组织头像文件id不能为空")
    @Schema(description = "组织头像文件id", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long avatarFileId;

    @NotBlank(message = "组织简介不能为空")
    @Schema(description = "组织简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}

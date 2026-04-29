package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "组织管理员端更新当前组织信息请求参数")
public class OrgAdminUpdateCurrentOrganizationInfoDTO {

    @NotBlank(message = "组织名称不能为空")
    @Size(max = 50, message = "组织名称不能超过 50 个字符")
    @Schema(description = "组织名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "组织头像文件ID不能为空")
    @Schema(type = "string", description = "组织头像文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long avatarFileId;

    @NotBlank(message = "组织简介不能为空")
    @Size(max = 500, message = "组织简介不能超过 500 个字符")
    @Schema(description = "组织简介", requiredMode = Schema.RequiredMode.REQUIRED)
    private String description;
}

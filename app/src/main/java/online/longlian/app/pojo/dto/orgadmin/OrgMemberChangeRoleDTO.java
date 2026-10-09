package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "组织成员角色变更请求")
public class OrgMemberChangeRoleDTO {

    @NotBlank(message = "组织角色不能为空")
    @Pattern(regexp = "ORG_ADMIN|ORG_USER", message = "组织角色仅支持 ORG_ADMIN 或 ORG_USER")
    @Schema(description = "组织角色", allowableValues = {"ORG_ADMIN", "ORG_USER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private String orgRole;
}

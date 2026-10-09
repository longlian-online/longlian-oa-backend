package online.longlian.app.pojo.dto.orgadmin;

import com.alibaba.fastjson2.annotation.JSONField;

import jakarta.validation.constraints.AssertTrue;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.enumeration.OrganizationRole;

@Data
@Schema(description = "组织成员角色变更请求")
public class OrgMemberChangeRoleDTO {
    @NotNull(message = "组织角色不能为空")
    @Schema(description = "组织角色", allowableValues = {"ORG_ADMIN", "ORG_USER"}, requiredMode = Schema.RequiredMode.REQUIRED)
    private OrganizationRole orgRole;
    @AssertTrue(message = "组织角色仅支持 ORG_ADMIN 或 ORG_USER")
    @Schema(hidden = true)
    @JSONField(serialize = false, deserialize = false)
    public boolean isAssignableRole() {
        return orgRole == null || orgRole != OrganizationRole.ORG_OWNER;
    }
}

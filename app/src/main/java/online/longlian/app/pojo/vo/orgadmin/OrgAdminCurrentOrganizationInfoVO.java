package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "组织管理员端当前组织信息")
public class OrgAdminCurrentOrganizationInfoVO {

    @Schema(type = "string", description = "组织ID")
    private Long id;

    @Schema(description = "组织名称")
    private String name;

    @Schema(description = "组织头像访问地址")
    private String avatarUrl;

    @Schema(description = "组织简介")
    private String description;
}

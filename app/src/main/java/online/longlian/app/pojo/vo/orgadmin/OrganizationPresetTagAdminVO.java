package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "组织预制标签管理列表信息")
public class OrganizationPresetTagAdminVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织预制标签ID")
    private Long id;

    @Schema(description = "标签名")
    private String name;

    @Schema(description = "颜色，空为默认。blue、green、amber、rose、violet")
    private String color;

    @Schema(description = "优先级，仅影响用户端下拉顺序，越大越靠前")
    private Integer priority;
}

package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;

@NoArgsConstructor
@Data
@AllArgsConstructor
@Builder
@Schema(description = "组织预制标签")
public class OrganizationPresetTagVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织预制标签ID")
    private Long id;

    @Schema(description = "标签名")
    private String name;

    @Schema(description = "颜色，空为默认。blue、green、amber、rose、violet")
    private String color;
}

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
@Schema(description = "组织预置表单字段")
public class OrganizationPresetFieldVO {

    @JsonLongIdString
    @Schema(type = "string", description = "组织预置表单字段ID")
    private Long id;

    @Schema(description = "字段名")
    private String name;
}

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
@Schema(description = "企划类型默认自定义表单中的一项")
public class ProjectTypeCustomFieldVO {

    @JsonLongIdString
    @Schema(type = "string", description = "预置表单字段ID")
    private Long presetFieldId;

    @Schema(description = "字段名")
    private String name;

    @Schema(description = "是否必填")
    private Boolean required;
}

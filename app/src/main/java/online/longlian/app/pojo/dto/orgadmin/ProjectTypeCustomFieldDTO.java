package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "企划类型默认自定义表单中的一项")
public class ProjectTypeCustomFieldDTO {

    @NotNull(message = "预置表单字段不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "预置表单字段ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long presetFieldId;

    @NotNull(message = "是否必填不能为空")
    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean required;
}

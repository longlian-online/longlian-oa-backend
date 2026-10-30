package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "企划类型默认自定义表单中的一项")
public class ProjectTypeCustomFieldDTO {

    @JsonLongIdString
    @Schema(type = "string", description = "预置表单字段ID；自行编辑的字段不传")
    private Long presetFieldId;

    @Size(max = 50, message = "字段名不能超过 50 个字符")
    @Schema(description = "字段名；引用预置字段时忽略，自行编辑时必填")
    private String name;

    @NotNull(message = "是否必填不能为空")
    @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean required;
}

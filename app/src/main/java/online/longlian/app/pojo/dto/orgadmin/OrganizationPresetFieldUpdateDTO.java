package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改组织预置表单字段请求参数")
public class OrganizationPresetFieldUpdateDTO {

    @NotBlank(message = "字段名不能为空")
    @Size(max = 50, message = "字段名不能超过 50 个字符")
    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}

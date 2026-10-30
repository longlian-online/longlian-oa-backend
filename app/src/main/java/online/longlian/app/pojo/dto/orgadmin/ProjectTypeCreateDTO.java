package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "创建企划类型请求参数")
public class ProjectTypeCreateDTO {

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 20, message = "类型名称不能超过 20 个字符")
    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "优先级，仅影响用户端类型列表顺序，越大越靠前，默认 0")
    private Integer priority;

    @Valid
    @Schema(description = "默认自定义表单。省略或空数组表示不设置")
    private List<@NotNull(message = "默认自定义表单不能包含空项") @Valid ProjectTypeCustomFieldDTO> customFields;
}

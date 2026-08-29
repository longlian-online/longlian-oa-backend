package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "修改企划类型请求参数")
public class ProjectTypeUpdateDTO {
    @NotBlank(message = "类型名称不能为空")
    @Size(max = 20, message = "类型名称不能超过 20 个字符")
    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}

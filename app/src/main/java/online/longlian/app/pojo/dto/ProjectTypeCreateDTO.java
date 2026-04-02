package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建企划类型请求参数")
public class ProjectTypeCreateDTO {

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 50, message = "类型名称不能超过 50 个字符")
    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
}

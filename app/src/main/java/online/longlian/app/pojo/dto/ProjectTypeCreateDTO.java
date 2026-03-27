package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "创建企划类型请求参数")
public class ProjectTypeCreateDTO {

    @NotBlank(message = "类型名称不能为空")
    @Size(max = 50, message = "类型名称不能超过 50 个字符")
    @Schema(description = "类型名称", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @NotNull(message = "排序不能为空")
    @Min(value = 0, message = "排序不能小于 0")
    @Schema(description = "排序（数字越小越靠前）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;
}

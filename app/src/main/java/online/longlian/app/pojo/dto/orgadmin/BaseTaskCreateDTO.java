package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.pojo.bo.common.TaskFormField;
import java.util.List;

@Data
@Schema(description = "创建/更新原子任务请求参数")
public class BaseTaskCreateDTO {

    @NotBlank(message = "任务名称不能为空")
    @Size(max = 100, message = "任务名称不能超过 100 个字符")
    @Schema(description = "任务名称（如：创建/翻译/校对）", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Size(max = 500, message = "任务说明不能超过 500 个字符")
    @Schema(description = "任务说明")
    private String description;

    @Size(max = 100, message = "Lucide 图标组件名不能超过 100 个字符")
    @Schema(description = "Lucide 图标组件名")
    private String icon;

    @NotNull
    @Schema(description = "提交表单字段", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<TaskFormField> submitFields;
}

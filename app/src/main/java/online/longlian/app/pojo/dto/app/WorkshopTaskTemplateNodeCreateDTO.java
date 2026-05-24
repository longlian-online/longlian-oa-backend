package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "工坊任务流模板节点（创建模板时使用）")
public class WorkshopTaskTemplateNodeCreateDTO {

    @NotNull(message = "原子任务ID不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "关联的原子任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long baseTaskId;

    @Size(max = 50, message = "任务实例名不能超过 50 个字符")
    @Schema(description = "自定义任务实例名，用于区分同一原子任务的多个并行实例")
    private String customName;

    @NotNull(message = "步骤顺序不能为空")
    @Min(value = 0, message = "步骤顺序不能小于 0")
    @Schema(description = "步骤顺序（同一 sort 值的节点为并行节点）", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    @Schema(description = "并行组内排序（sort 相同时，用于控制并行节点的展示顺序）")
    private Integer parallelSort;
}

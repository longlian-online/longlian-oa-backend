package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "任务模板节点（创建/更新模板时使用）")
public class TaskTemplateNodeDTO {

    @NotNull(message = "原子任务ID不能为空")
    @Schema(description = "关联的原子任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long baseTaskId;

    @NotNull(message = "步骤顺序不能为空")
    @Min(value = 0, message = "步骤顺序不能小于 0")
    @Schema(
        description = "步骤顺序（同一 sort 值的节点为并行节点）",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer sort;

    @Schema(description = "并行组内排序（sort 相同时，用于控制并行节点的展示顺序）")
    private Integer parallelSort;
}

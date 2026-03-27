package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "为任务流单独新增节点请求参数")
public class ItemTaskNodeAddDTO {

    @NotNull(message = "原子任务ID不能为空")
    @Schema(description = "关联的原子任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long baseTaskId;

    @NotNull(message = "步骤顺序不能为空")
    @Min(value = 0, message = "步骤顺序不能小于 0")
    @Schema(
        description = "步骤顺序（与已有节点 sort 相同则表示并行加入该组，新 sort 值则插入新步骤）",
        requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Integer sort;

    @Schema(description = "并行组内排序（sort 相同时，控制并行节点的展示顺序）")
    private Integer parallelSort;
}

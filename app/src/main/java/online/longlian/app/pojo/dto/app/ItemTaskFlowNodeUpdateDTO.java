package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "项目工作流节点")
public class ItemTaskFlowNodeUpdateDTO {

    @JsonLongIdString
    @Schema(type = "string", description = "已有节点 ID。新节点不传")
    private Long id;

    @Schema(description = "已有节点的任务修订号。新节点不传")
    private String revision;

    @NotNull(message = "任务不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "原子任务 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long baseTaskId;

    @NotNull(message = "步骤顺序不能为空")
    @Min(value = 1, message = "步骤顺序不能小于 1")
    @Schema(description = "步骤顺序，相同值表示并行", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    @NotNull(message = "并行顺序不能为空")
    @Min(value = 1, message = "并行顺序不能小于 1")
    @Schema(description = "并行组内顺序", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parallelSort;

    @Size(max = 100, message = "节点名称不能超过 100 个字符")
    @Schema(description = "节点名称。留空时已有节点保持原名，新节点使用原子任务名称")
    private String name;
}

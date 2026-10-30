package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "创建项目时的流程节点")
public class ProjectItemNodeCreateDTO {

    @NotNull(message = "原子任务不能为空")
    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long baseTaskId;

    @NotNull(message = "流程位置无效")
    @Min(value = 1, message = "流程位置无效")
    @Schema(description = "阶段序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer sort;

    @NotNull(message = "流程位置无效")
    @Min(value = 1, message = "流程位置无效")
    @Schema(description = "阶段内序号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer parallelSort;

    @Size(max = 100, message = "节点名称不能超过 100 个字符")
    @Schema(description = "节点名称，空则使用原子任务名称")
    private String name;

    @JsonLongIdString
    @Schema(type = "string", description = "执行人用户ID，空表示待接取")
    private Long assigneeId;
}

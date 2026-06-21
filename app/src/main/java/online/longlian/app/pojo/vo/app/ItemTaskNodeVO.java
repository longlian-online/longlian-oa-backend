package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.TaskInstanceStatus;

import io.swagger.v3.oas.annotations.media.Schema;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务流节点信息（含执行状态，用于流程图可视化）")
public class ItemTaskNodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务节点ID")
    private Long id;

    @JsonLongIdString
    @Schema(type = "string", description = "关联原子任务ID")
    private Long baseTaskId;

    @Schema(description = "任务名称")
    private String name;

    @Schema(description = "原子任务图标URL")
    private String baseTaskIconUrl;

    @Schema(description = "节点元数据字段定义快照(JSON数组)")
    private String metaSchema;

    @Schema(description = "步骤顺序（相同 sort 值为并行节点）")
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例ID，null 表示该节点尚未生成实例（前序未完成）")
    private Long taskInstanceId;

    @Schema(description = "任务实例状态：PENDING(待接取)/CLAIMED(待提交)/COMPLETED(已完成)，null 表示节点未解锁")
    private TaskInstanceStatus taskStatus;
}

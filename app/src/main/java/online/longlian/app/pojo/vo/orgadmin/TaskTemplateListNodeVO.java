package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "任务模板列表阶段预览，不含提交字段")
public class TaskTemplateListNodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "关联原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称，作为阶段名")
    private String baseTaskName;

    @Schema(description = "原子任务 Lucide 图标组件名")
    private String baseTaskIcon;

    @Schema(description = "步骤顺序（相同 sort 表示并行节点）")
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;
}

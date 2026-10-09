package online.longlian.app.pojo.vo.orgadmin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.pojo.bo.common.TaskFormField;
import java.util.List;

@Data
@Schema(description = "任务模板节点信息")
public class TaskTemplateNodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "模板节点ID")
    private Long id;

    @JsonLongIdString
    @Schema(type = "string", description = "关联原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称")
    private String baseTaskName;

    @Schema(description = "原子任务图标URL")
    private String baseTaskIconUrl;

    @Schema(description = "原子任务 Lucide 图标组件名")
    private String baseTaskIconName;

    @Schema(description = "提交表单字段")
    private List<TaskFormField> submitFields;

    @Schema(
        description = "步骤顺序（相同 sort 值表示并行节点）"
    )
    private Integer sort;

    @Schema(description = "并行组内排序")
    private Integer parallelSort;
}

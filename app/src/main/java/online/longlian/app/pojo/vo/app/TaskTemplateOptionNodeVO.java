package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.Status;

@Data
@Schema(description = "创建项目时可选模板的步骤")
public class TaskTemplateOptionNodeVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long baseTaskId;

    @Schema(description = "原子任务名称")
    private String baseTaskName;

    @Schema(description = "原子任务状态：ENABLED-启用，DISABLED-禁用")
    private Status baseTaskStatus;

    @Schema(description = "阶段序号")
    private Integer sort;

    @Schema(description = "阶段内序号")
    private Integer parallelSort;
}

package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.common.TaskFormField;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "任务草稿详情")
public class TaskDraftDetailVO {

    @Schema(description = "任务当前版本")
    private TaskDraftTaskVO task;

    @Schema(description = "前置阶段已完成，可以正式提交")
    private boolean canSubmit;

    @Schema(description = "节点提交表单定义")
    private List<TaskFormField> submitFields;

    @Schema(description = "当前接取人的有效草稿；尚未暂存为null")
    private TaskDraftContentVO draft;
}

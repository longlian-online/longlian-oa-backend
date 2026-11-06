package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "草稿正式提交结果")
public class TaskDraftSubmitResultVO {

    @Schema(description = "任务实例ID")
    private String instanceId;

    @Schema(description = "提交后的任务修订号")
    private String taskRevision;

    @Schema(description = "正式提交记录ID")
    private String submissionId;

    @Schema(description = "提交后的任务状态")
    private String status;
}

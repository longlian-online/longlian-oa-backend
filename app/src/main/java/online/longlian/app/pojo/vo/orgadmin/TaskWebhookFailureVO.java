package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Schema(description = "失败的任务 webhook 投递")
public class TaskWebhookFailureVO {
    @JsonLongIdString
    @Schema(type = "string", description = "投递 ID")
    private Long id;

    @Schema(description = "task.submitted 或 task.submission_invalidated")
    private String event;

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例 ID")
    private Long taskInstanceId;

    @JsonLongIdString
    @Schema(type = "string", description = "提交 ID")
    private Long submissionId;

    @Schema(description = "最近一次失败原因")
    private String lastError;

    @Schema(description = "已领取次数")
    private Integer attemptCount;

    @Schema(description = "最近更新时间")
    private LocalDateTime updatedAt;
}

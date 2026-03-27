package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.TaskSubmissionStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务提交记录信息（提交列表页）")
public class TaskSubmissionVO {

    @Schema(description = "提交记录ID")
    private Long id;

    @Schema(description = "任务实例ID")
    private Long taskInstanceId;

    @Schema(description = "任务类型名称（如：嵌字、翻译）")
    private String baseTaskName;

    @Schema(description = "提交状态")
    private TaskSubmissionStatus status;

    @Schema(description = "提交人ID")
    private Long submitterId;

    @Schema(description = "提交人昵称")
    private String submitterNickname;

    @Schema(description = "提交人头像URL")
    private String submitterAvatarUrl;

    @Schema(description = "上传文件ID")
    private Long fileId;

    @Schema(description = "上传文件名")
    private String fileName;

    @Schema(description = "审核人ID（打回操作人）")
    private Long reviewerId;

    @Schema(description = "审核人昵称")
    private String reviewerNickname;

    @Schema(description = "打回意见")
    private String reviewComment;

    @Schema(description = "提交时间")
    private LocalDateTime createdAt;

    @Schema(description = "审核/打回时间")
    private LocalDateTime reviewedAt;
}

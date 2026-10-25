package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "打回任务请求参数")
public class TaskRejectDTO {

    @NotBlank(message = "任务修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "任务修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775807", message = "任务修订号超出范围")
    @Schema(description = "当前任务列表中的 revision，旧修订号不会执行操作", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedRevision;

    @NotBlank(message = "打回意见不能为空")
    @Size(max = 500, message = "打回意见不能超过 500 个字符")
    @Schema(description = "打回意见", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewComment;

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment == null ? null : reviewComment.trim();
    }
}

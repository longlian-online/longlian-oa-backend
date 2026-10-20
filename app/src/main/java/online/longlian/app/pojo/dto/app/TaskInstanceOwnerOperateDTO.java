package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "企划负责人取消接取或打回提交请求")
public class TaskInstanceOwnerOperateDTO {

    @NotBlank(message = "任务修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "任务修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775807", message = "任务修订号超出范围")
    @Schema(description = "当前任务列表中的 revision，旧修订号不会执行操作", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedRevision;

    @NotBlank(message = "操作原因不能为空")
    @Size(min = 1, max = 500, message = "操作原因须为 1 到 500 个字符")
    @Schema(description = "操作原因，去除首尾空白后为 1-500 个字符", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reason;

    public void setReason(String reason) {
        this.reason = reason == null ? null : reason.trim();
    }
}

package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "放弃任务或取消接取请求")
public class TaskAbandonDTO {

    @NotBlank(message = "任务修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "任务修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775807", message = "任务修订号超出范围")
    @Schema(description = "当前任务列表中的 revision，旧修订号不会执行操作", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedRevision;
}

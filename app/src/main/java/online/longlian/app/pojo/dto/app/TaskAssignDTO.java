package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "企划负责人分配未接取任务")
public class TaskAssignDTO {

    @NotNull(message = "组员不能为空")
    @Positive(message = "组员ID不合法")
    @Schema(type = "string", description = "被分配的组织成员用户ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assigneeId;

    @NotBlank(message = "任务修订号不能为空")
    @Pattern(regexp = "0|[1-9][0-9]{0,18}", message = "任务修订号必须为非负整数字符串")
    @DecimalMax(value = "9223372036854775807", message = "任务修订号超出范围")
    @Schema(description = "当前任务列表中的 revision，旧修订号不会执行操作", example = "0",
            requiredMode = Schema.RequiredMode.REQUIRED)
    private String expectedRevision;
}

package online.longlian.app.pojo.vo.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
@Schema(description = "定时任务列表项")
public class ScheduledTaskVO {

    @Schema(description = "任务唯一标识")
    private String taskName;

    @Schema(description = "任务描述")
    private String description;

    @Schema(description = "cron 表达式，为 null 表示仅支持手动触发")
    private String cronExpression;

    @Schema(description = "是否启用定时调度")
    private boolean enabled;
}

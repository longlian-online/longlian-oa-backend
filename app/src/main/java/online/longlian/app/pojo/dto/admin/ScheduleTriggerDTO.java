package online.longlian.app.pojo.dto.admin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Schema(description = "手动触发定时任务请求")
public class ScheduleTriggerDTO {

    @Schema(description = "上层传递的执行时间，为 null 时默认使用当前时间")
    private LocalDateTime executeTime;
}

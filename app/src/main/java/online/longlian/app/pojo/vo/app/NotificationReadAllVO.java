package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "全部标记已读结果")
public class NotificationReadAllVO {

    @Schema(type = "string", example = "2026-10-22 12:00:00", description = "全部标记为已读的时间")
    private LocalDateTime readAt;
}

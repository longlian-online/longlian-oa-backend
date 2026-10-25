package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class NotificationReadAllVO {
    @Schema(type = "string", example = "2026-10-22 12:00:00")
    private LocalDateTime readAt;
}

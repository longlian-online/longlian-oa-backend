package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@Schema(description = "通知已读结果")
public class NotificationReadVO {

    @JsonLongIdString
    @Schema(type = "string", description = "通知ID")
    private Long id;

    @Schema(type = "string", example = "2026-10-22 12:00:00", description = "已读时间")
    private LocalDateTime readAt;
}

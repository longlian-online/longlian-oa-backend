package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Schema(description = "通知列表游标参数")
public class NotificationListDTO {

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(type = "string", example = "2026-10-22 12:00:00", description = "游标：上一页最后一条通知的创建时间")
    private LocalDateTime beforeCreatedAt;

    @Positive
    @Schema(type = "string", description = "游标：上一页最后一条通知ID")
    private Long beforeId;

    @Schema(description = "游标：上一页最后一条通知是否已读")
    private Boolean beforeUnread;

    @AssertTrue(message = "通知游标参数必须同时提供")
    @Schema(hidden = true)
    public boolean isCursorPaired() {
        return (beforeCreatedAt == null) == (beforeId == null) && (beforeId == null) == (beforeUnread == null);
    }
}

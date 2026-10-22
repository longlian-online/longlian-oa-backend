package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;

@Data
public class NotificationListDTO {
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(type = "string", example = "2026-10-22 12:00:00")
    private LocalDateTime beforeCreatedAt;
    @Positive
    private Long beforeId;

    @AssertTrue(message = "通知游标参数必须成对提供")
    @Schema(hidden = true)
    public boolean isCursorPaired() {
        return (beforeCreatedAt == null) == (beforeId == null);
    }
}

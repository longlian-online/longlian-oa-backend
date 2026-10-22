package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import online.longlian.common.enumeration.InboxNotificationKind;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResultBO {
    private Long id;
    private InboxNotificationKind kind;
    private Long sourceId;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private NotificationContentResultBO content;
}

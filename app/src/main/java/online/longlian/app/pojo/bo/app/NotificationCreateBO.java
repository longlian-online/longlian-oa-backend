package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.InboxNotificationKind;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateBO {
    private Long recipientUserId;
    private InboxNotificationKind kind;
    private Long sourceId;
    private Long triggerId;
    private LocalDateTime createdAt;
}

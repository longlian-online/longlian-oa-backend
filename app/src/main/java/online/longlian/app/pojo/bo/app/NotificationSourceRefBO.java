package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.InboxNotificationKind;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSourceRefBO {
    private Long notificationId;
    private InboxNotificationKind kind;
    private Long sourceId;
}

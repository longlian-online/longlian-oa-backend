package online.longlian.app.service.app.impl.notification;

import online.longlian.common.enumeration.InboxNotificationKind;

public record InboxEmailPayload(
        Long notificationId,
        Long orgId,
        Long recipientUserId,
        InboxNotificationKind kind,
        Long sourceId) {
}

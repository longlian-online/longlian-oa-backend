package online.longlian.app.service.app.impl.notification;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.NotificationCreateBO;
import online.longlian.app.service.app.InboxNotificationService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.InboxNotificationKind;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdminReviewNotificationPublisher {
    private final OrganizationMembershipService organizationMembershipService;
    private final InboxNotificationService inboxNotificationService;

    public void publish(Long orgId, InboxNotificationKind kind, Long sourceId, LocalDateTime createdAt) {
        if (sourceId == null) {
            throw new IllegalStateException("审核通知缺少来源");
        }
        List<NotificationCreateBO> notifications = organizationMembershipService.listEnabledAdministratorIds(orgId)
                .stream()
                .map(userId -> new NotificationCreateBO(userId, kind, sourceId, sourceId, createdAt))
                .toList();
        inboxNotificationService.createBatch(new NotificationBatchCreateParamsBO(orgId, notifications));
    }
}

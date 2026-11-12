package online.longlian.app.service.app.notification.impl;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.app.notification.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationCreateBO;
import online.longlian.app.service.app.notification.InboxNotificationService;
import online.longlian.app.service.organization.OrganizationMembershipService;
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

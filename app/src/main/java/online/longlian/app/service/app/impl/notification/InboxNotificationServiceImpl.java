package online.longlian.app.service.app.impl.notification;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import online.longlian.app.mapper.UserNotificationMapper;
import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.entity.UserNotification;
import online.longlian.app.service.app.InboxNotificationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InboxNotificationServiceImpl extends ServiceImpl<UserNotificationMapper, UserNotification>
        implements InboxNotificationService {
    @Override
    @Transactional(propagation = Propagation.MANDATORY, rollbackFor = Exception.class)
    public void createBatch(NotificationBatchCreateParamsBO params) {
        if (params.getNotifications().isEmpty()) {
            return;
        }
        saveBatch(params.getNotifications().stream().map(notification -> UserNotification.builder()
                .orgId(params.getOrgId())
                .recipientUserId(notification.getRecipientUserId())
                .kind(notification.getKind())
                .sourceId(notification.getSourceId())
                .triggerId(notification.getTriggerId())
                .createdAt(notification.getCreatedAt())
                .updatedAt(notification.getCreatedAt())
                .build()).toList());
    }
}

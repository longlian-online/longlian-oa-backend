package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.notification.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationStatusParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationStatusResultBO;
import online.longlian.app.pojo.bo.app.notification.NotificationListParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationPageResultBO;
import online.longlian.app.pojo.bo.app.notification.NotificationReadParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationReadResultBO;
import online.longlian.app.pojo.bo.app.notification.NotificationReadAllParamsBO;
import online.longlian.app.pojo.bo.app.notification.NotificationReadAllResultBO;

public interface InboxNotificationService {
    void createBatch(NotificationBatchCreateParamsBO params);
    NotificationStatusResultBO getStatus(NotificationStatusParamsBO params);
    NotificationPageResultBO list(NotificationListParamsBO params);
    NotificationReadResultBO markRead(NotificationReadParamsBO params);
    NotificationReadAllResultBO markAllRead(NotificationReadAllParamsBO params);
}

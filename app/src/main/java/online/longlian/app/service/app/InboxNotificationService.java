package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;
import online.longlian.app.pojo.bo.app.NotificationStatusParamsBO;
import online.longlian.app.pojo.bo.app.NotificationStatusResultBO;
import online.longlian.app.pojo.bo.app.NotificationListParamsBO;
import online.longlian.app.pojo.bo.app.NotificationPageResultBO;
import online.longlian.app.pojo.bo.app.NotificationReadParamsBO;
import online.longlian.app.pojo.bo.app.NotificationReadResultBO;
import online.longlian.app.pojo.bo.app.NotificationReadAllParamsBO;
import online.longlian.app.pojo.bo.app.NotificationReadAllResultBO;

public interface InboxNotificationService {
    void createBatch(NotificationBatchCreateParamsBO params);
    NotificationStatusResultBO getStatus(NotificationStatusParamsBO params);
    NotificationPageResultBO list(NotificationListParamsBO params);
    NotificationReadResultBO markRead(NotificationReadParamsBO params);
    NotificationReadAllResultBO markAllRead(NotificationReadAllParamsBO params);
}

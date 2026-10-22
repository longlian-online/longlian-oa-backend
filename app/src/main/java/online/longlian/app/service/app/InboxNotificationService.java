package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.NotificationBatchCreateParamsBO;

public interface InboxNotificationService {
    void createBatch(NotificationBatchCreateParamsBO params);
}

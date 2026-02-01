package online.longlian.app.service.notify;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.pojo.dto.NotificationReqDTO;

public interface NotificationService {

    /**
     * 当前实现支持的通知类型
     */
    NotificationType getType();

    /**
     * 发送通知
     */
    void send(NotificationReqDTO request);
}
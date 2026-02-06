package online.longlian.app.service.notify;

import online.longlian.app.common.enumeration.NotificationType;

public interface NotificationService {

    /**
     * 当前实现支持的通知类型
     */
    NotificationType getType();

    /**
     * 发送通知
     */
    void send(String receiver,String code);
}
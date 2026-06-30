package online.longlian.app.service.notify.impl;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.service.notify.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class NullNotificationService implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NullNotificationService.class);

    @Override
    public NotificationType getType() {
        return NotificationType.NOOP;
    }

    @Override
    public void send(String receiver, String code) {
        log.warn("空通知实现生效，receiver={}, code={}", receiver, code);
    }
}
package online.longlian.app.service.notify;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.pojo.dto.NotificationReqDTO;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class NotificationManager {


    public List<NotificationService> notificationServices;

    private final Map<NotificationType, NotificationService> serviceMap = new HashMap<>();

    @Value("${notify.type}")
    private NotificationType notificationType;

    public NotificationManager(List<NotificationService> notificationServices) {
        this.notificationServices = notificationServices;
    }

    @PostConstruct
    public void init() {
        for (NotificationService service : notificationServices) {
            serviceMap.put(service.getType(), service);
        }
    }

    public void send(NotificationReqDTO request) {
        NotificationService service = serviceMap.get(notificationType);
        if (service == null) {
            throw new IllegalArgumentException("未实现的通知类型: " + notificationType);
        }
        service.send(request);
    }
}
package online.longlian.app.service.notify;

import online.longlian.app.common.enumeration.NotificationType;

import jakarta.annotation.PostConstruct;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
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

    public void send(String receiver,String code) {
        NotificationService service = serviceMap.get(notificationType);
        if (service == null) {
            throw new AppException(ResultCode.OPERATION_FAIL);
        }
        service.send(receiver,code);
    }
}
package online.longlian.app;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.pojo.dto.NotificationRequest;
import online.longlian.app.service.notify.NotificationManager;
import online.longlian.app.service.notify.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Value;

import java.util.Collections;

import static org.mockito.Mockito.*;

class NotificationManagerTest {

    private NotificationManager notificationManager;
    private NotificationService emailService;

    @Value("${notify.type}")
    private NotificationType notificationType;
    @BeforeEach
    void setUp() {
        // 创建 Mock 邮件服务
        emailService = Mockito.mock(NotificationService.class);
        when(emailService.getType()).thenReturn(notificationType);

        // 初始化 NotificationManager
        notificationManager = new NotificationManager(Collections.singletonList(emailService));
        notificationManager.init(); // 手动触发 @PostConstruct
    }

    @Test
    void testSendEmail() {
        // 构建通知请求
        NotificationRequest request = new NotificationRequest();
        request.setReceiver("test@qq.com");
        request.setTitle("测试邮件");
        request.setContent("这是一封测试邮件");

        // 调用 send
        notificationManager.send(request);

        // 验证 emailService.send 被调用一次
        verify(emailService, times(1)).send(request);
    }
    
}

package online.longlian.app.service.notify;

import online.longlian.app.common.enumeration.NotificationType;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.service.notify.impl.EmailNotificationService;
import online.longlian.app.common.util.MailUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationManagerUnitTest {

    @Mock
    private NotificationService emailService;

    private NotificationManager manager;

    @BeforeEach
    void setUp() {
        when(emailService.getType()).thenReturn(NotificationType.EMAIL);
        manager = new NotificationManager(List.of(emailService));
        ReflectionTestUtils.setField(manager, "notificationType", NotificationType.EMAIL);
        manager.init();
    }

    @Test
    void send_validType_delegatesToService() {
        manager.send("user@test.com", "123456");
        verify(emailService).send("user@test.com", "123456");
    }

    @Test
    void send_unregisteredType_throws() {
        ReflectionTestUtils.setField(manager, "notificationType", NotificationType.SMS);

        assertThatThrownBy(() -> manager.send("user@test.com", "123456"))
                .isInstanceOf(AppException.class);
    }
}

package online.longlian.app.service.otp.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.app.service.notify.NotificationManager;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerifyCodeAsyncSenderTest {

    @Mock
    private EmailVerifyOtpMapper emailVerifyOtpMapper;
    @Mock
    private NotificationManager notificationManager;

    private EmailVerifyCodeAsyncSender sender;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), EmailVerifyOtp.class);
        sender = new EmailVerifyCodeAsyncSender(emailVerifyOtpMapper, notificationManager);
    }

    @Test
    void send_success_marksSent() {
        doNothing().when(notificationManager).send("user@example.com", "123456");
        when(emailVerifyOtpMapper.update(isNull(), any())).thenReturn(1);

        sender.send(1L, "user@example.com", "123456");

        verify(notificationManager).send("user@example.com", "123456");
        verify(emailVerifyOtpMapper).update(isNull(), any());
    }

    @Test
    void send_failure_marksFailed() {
        doThrow(new RuntimeException("SMTP error")).when(notificationManager).send(anyString(), anyString());
        when(emailVerifyOtpMapper.update(isNull(), any())).thenReturn(1);

        sender.send(1L, "user@example.com", "123456");

        verify(emailVerifyOtpMapper).update(isNull(), any());
    }

    @Test
    void send_failureWithNullMessage_usesClassName() {
        doThrow(new RuntimeException((String) null)).when(notificationManager).send(anyString(), anyString());
        when(emailVerifyOtpMapper.update(isNull(), any())).thenReturn(1);

        sender.send(1L, "user@example.com", "123456");

        verify(emailVerifyOtpMapper).update(isNull(), any());
    }

    @Test
    void send_failureWithLongMessage_truncates() {
        String longMsg = "x".repeat(600);
        doThrow(new RuntimeException(longMsg)).when(notificationManager).send(anyString(), anyString());
        when(emailVerifyOtpMapper.update(isNull(), any())).thenReturn(1);

        sender.send(1L, "user@example.com", "123456");

        verify(emailVerifyOtpMapper).update(isNull(), any());
    }
}

package online.longlian.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.app.service.notify.NotificationManager;
import online.longlian.common.enumeration.EmailVerifySendStatus;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailVerifyCodeAsyncSender {

    private static final int MAX_FAIL_REASON_LENGTH = 500;

    private final EmailVerifyOtpMapper emailVerifyOtpMapper;
    private final NotificationManager notificationManager;

    @Async("verifyCodeExecutor")
    public void send(Long emailVerifyOtpId, String receiver, String code) {
        try {
            notificationManager.send(receiver, code);
            markSent(emailVerifyOtpId);
        } catch (Exception e) {
            markFailed(emailVerifyOtpId, e);
            log.error("{} 验证码发送失败", receiver, e);
        }
    }

    private void markSent(Long emailVerifyOtpId) {
        LocalDateTime now = LocalDateTime.now();
        emailVerifyOtpMapper.update(
                null,
                new LambdaUpdateWrapper<EmailVerifyOtp>()
                        .eq(EmailVerifyOtp::getId, emailVerifyOtpId)
                        .eq(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.PENDING)
                        .set(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.SENT)
                        .set(EmailVerifyOtp::getSentAt, now)
                        .set(EmailVerifyOtp::getUpdatedAt, now)
        );
    }

    private void markFailed(Long emailVerifyOtpId, Exception e) {
        LocalDateTime now = LocalDateTime.now();
        emailVerifyOtpMapper.update(
                null,
                new LambdaUpdateWrapper<EmailVerifyOtp>()
                        .eq(EmailVerifyOtp::getId, emailVerifyOtpId)
                        .eq(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.PENDING)
                        .set(EmailVerifyOtp::getSendStatus, EmailVerifySendStatus.FAILED)
                        .set(EmailVerifyOtp::getFailedAt, now)
                        .set(EmailVerifyOtp::getFailReason, normalizeFailReason(e))
                        .set(EmailVerifyOtp::getUpdatedAt, now)
        );
    }

    private String normalizeFailReason(Exception e) {
        String reason = e.getMessage();
        if (reason == null || reason.isBlank()) {
            reason = e.getClass().getSimpleName();
        }
        return reason.length() > MAX_FAIL_REASON_LENGTH ? reason.substring(0, MAX_FAIL_REASON_LENGTH) : reason;
    }
}

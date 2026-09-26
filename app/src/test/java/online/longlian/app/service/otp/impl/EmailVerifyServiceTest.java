package online.longlian.app.service.otp.impl;

import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerifyServiceTest {

    @Mock
    private OneTimePasswordService oneTimePasswordService;
    @Mock
    private EmailVerifyOtpMapper emailVerifyOtpMapper;
    @Mock
    private EmailVerifyCodeAsyncSender emailVerifyCodeAsyncSender;

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    @Test
    void shouldSendEmailOnlyAfterTransactionCommit() {
        when(emailVerifyOtpMapper.selectCount(any())).thenReturn(0L);
        when(oneTimePasswordService.generateOTP(any()))
                .thenReturn(OneTimePassword.builder().id(1L).build());
        assignEmailVerifyOtpId();

        TransactionSynchronizationManager.initSynchronization();
        service().generate(params());

        verify(emailVerifyCodeAsyncSender, never()).send(any(), anyString(), anyString());
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        verify(emailVerifyCodeAsyncSender).send(eq(2L), eq("user@example.com"), anyString());
    }

    @Test
    void shouldRejectGenerateWithoutTransactionSynchronization() {
        when(emailVerifyOtpMapper.selectCount(any())).thenReturn(0L);
        when(oneTimePasswordService.generateOTP(any()))
                .thenReturn(OneTimePassword.builder().id(1L).build());

        assertThatThrownBy(() -> service().generate(params()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Transaction synchronization is not active");
        verify(emailVerifyCodeAsyncSender, never()).send(any(), anyString(), anyString());
    }

    private EmailVerifyService service() {
        return new EmailVerifyService(
                oneTimePasswordService,
                emailVerifyOtpMapper,
                emailVerifyCodeAsyncSender,
                Clock.systemUTC()
        );
    }

    private void assignEmailVerifyOtpId() {
        doAnswer(invocation -> {
            invocation.<EmailVerifyOtp>getArgument(0).setId(2L);
            return 1;
        }).when(emailVerifyOtpMapper).insert(any(EmailVerifyOtp.class));
    }

    private OTPGenerateContextBO params() {
        return OTPGenerateContextBO.builder()
                .receiver("user@example.com")
                .businessType(EmailVerifyBusinessType.LOGIN)
                .creatorId(1L)
                .build();
    }
}

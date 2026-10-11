package online.longlian.app.service.otp.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.EmailVerifyOtpMapper;
import online.longlian.app.mapper.UserMapper;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class EmailVerifyServiceTest {

    @Mock
    private OneTimePasswordService oneTimePasswordService;
    @Mock
    private EmailVerifyOtpMapper emailVerifyOtpMapper;
    @Mock
    private UserMapper userMapper;
    @Mock
    private EmailVerifyCodeAsyncSender emailVerifyCodeAsyncSender;

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clear();
    }

    /** 非法收件人必须在持久化验证码或排队发送之前被拒绝。 */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "invalid-email"})
    void shouldRejectInvalidRecipientBeforeCreatingOrSendingCode(String receiver) {
        OTPGenerateContextBO context = OTPGenerateContextBO.builder()
                .receiver(receiver).businessType(EmailVerifyBusinessType.LOGIN).build();

        assertThatThrownBy(() -> service().generate(context)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(userMapper, emailVerifyOtpMapper, oneTimePasswordService, emailVerifyCodeAsyncSender);
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

    @Test
    void shouldRejectRegisterCodeForExistingEmailBeforeCreatingOtp() {
        when(userMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service().generate(params(EmailVerifyBusinessType.REGISTER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已存在");

        verify(emailVerifyOtpMapper, never()).selectCount(any());
        verify(oneTimePasswordService, never()).generateOTP(any());
        verify(emailVerifyOtpMapper, never()).insert(any(EmailVerifyOtp.class));
        verify(emailVerifyCodeAsyncSender, never()).send(any(), anyString(), anyString());
    }

    @Test
    void shouldAllowRegisterCodeForUnusedEmail() {
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(emailVerifyOtpMapper.selectCount(any())).thenReturn(0L);
        when(oneTimePasswordService.generateOTP(any()))
                .thenReturn(OneTimePassword.builder().id(1L).build());
        assignEmailVerifyOtpId();

        TransactionSynchronizationManager.initSynchronization();
        service().generate(params(EmailVerifyBusinessType.REGISTER));

        verify(oneTimePasswordService).generateOTP(any());
        verify(emailVerifyOtpMapper).insert(any(EmailVerifyOtp.class));
    }

    @Test
    void shouldAllowForgotPasswordCodeWithoutCheckingRegistration() {
        when(emailVerifyOtpMapper.selectCount(any())).thenReturn(0L);
        when(oneTimePasswordService.generateOTP(any()))
                .thenReturn(OneTimePassword.builder().id(1L).build());
        assignEmailVerifyOtpId();

        TransactionSynchronizationManager.initSynchronization();
        service().generate(params(EmailVerifyBusinessType.FORGOT_PASSWORD));

        verify(userMapper, never()).selectCount(any());
        verify(oneTimePasswordService).generateOTP(any());
    }

    private EmailVerifyService service() {
        return new EmailVerifyService(
                oneTimePasswordService,
                emailVerifyOtpMapper,
                userMapper,
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
        return params(EmailVerifyBusinessType.LOGIN);
    }

    private OTPGenerateContextBO params(EmailVerifyBusinessType businessType) {
        return OTPGenerateContextBO.builder()
                .receiver("user@example.com")
                .businessType(businessType)
                .creatorId(1L)
                .build();
    }
}

package online.longlian.app.service.app.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.UserResetPasswordParamsBO;
import online.longlian.app.pojo.bo.common.OTPUseContextBO;
import online.longlian.app.pojo.bo.common.OTPValidateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import online.longlian.common.enumeration.OTPType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private OrganizationMapper organizationMapper;
    @Mock
    private OrganizationMemberMapper organizationMemberMapper;
    @Mock
    private OrganizationJoinOtpMapper organizationJoinOtpMapper;
    @Mock
    private GroupApplicationMapper groupApplicationMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private CurrentOrganizationService currentOrganizationService;
    @Mock
    private OTPServiceFactory otpServiceFactory;
    @Mock
    private OTPStrategyService emailVerifyService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(
                passwordEncoder,
                organizationMapper,
                organizationMemberMapper,
                organizationJoinOtpMapper,
                groupApplicationMapper,
                resourceService,
                userMapper,
                currentOrganizationService,
                otpServiceFactory,
                Clock.systemUTC()
        );
    }

    @Test
    void shouldResetPasswordAndConsumeEmailCode() {
        UserResetPasswordParamsBO params = resetPasswordParams();
        OneTimePassword otp = OneTimePassword.builder().id(10L).build();
        User user = User.builder().id(1L).email(params.getEmail()).password("old-hash").build();
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class))).thenReturn(otp);
        when(userMapper.selectOne(any())).thenReturn(user);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        service.resetPassword(params);

        ArgumentCaptor<OTPValidateContextBO> validateCaptor = ArgumentCaptor.forClass(OTPValidateContextBO.class);
        verify(emailVerifyService).getValid(validateCaptor.capture());
        assertThat(validateCaptor.getValue().getTarget()).isEqualTo("user@example.com");
        assertThat(validateCaptor.getValue().getCode()).isEqualTo("A1B2C3");
        assertThat(validateCaptor.getValue().getBusinessType()).isEqualTo(EmailVerifyBusinessType.FORGOT_PASSWORD);
        assertThat(user.getPassword()).isEqualTo("new-hash");
        verify(userMapper).updateById(user);
        verify(emailVerifyService).use(argThat(context -> context.getOtpId().equals(10L)));
    }

    @Test
    void shouldFailResetPasswordWhenUserDoesNotExist() {
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class)))
                .thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.resetPassword(resetPasswordParams()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_EXIT.getCode());

        verify(userMapper, never()).updateById(any(User.class));
        verify(emailVerifyService, never()).use(any(OTPUseContextBO.class));
    }

    private UserResetPasswordParamsBO resetPasswordParams() {
        return UserResetPasswordParamsBO.builder()
                .email("user@example.com")
                .code("A1B2C3")
                .password("new-password")
                .build();
    }
}

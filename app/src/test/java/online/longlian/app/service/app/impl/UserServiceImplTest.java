package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.app.UserResetPasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserChangePasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserBindEmailParamsBO;
import online.longlian.app.pojo.bo.app.UserUpdateMyInfoParamsBO;
import online.longlian.app.pojo.bo.app.UserConfigParamsBO;
import online.longlian.app.pojo.bo.common.OTPUseContextBO;
import online.longlian.app.pojo.bo.common.OTPValidateContextBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.OrganizationInviteCode;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.bo.app.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgResultBO;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.invite.OrganizationInviteCodeService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.otp.OTPStrategyService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
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
    private OrganizationInviteCodeService organizationInviteCodeService;
    @Mock
    private GroupApplicationMapper groupApplicationMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private UserMapper userMapper;
    @Mock
    private OrganizationMembershipService organizationMembershipService;
    @Mock
    private OTPServiceFactory otpServiceFactory;
    @Mock
    private OTPStrategyService emailVerifyService;
    @Mock
    private SessionService sessionService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), GroupApplication.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Organization.class);
        service = new UserServiceImpl(
                passwordEncoder,
                organizationMapper,
                organizationMemberMapper,
                organizationInviteCodeService,
                groupApplicationMapper,
                resourceService,
                userMapper,
                organizationMembershipService,
                otpServiceFactory,
                Clock.systemUTC(),
                sessionService,
                tokenBlacklistService
        );
    }

    @ParameterizedTest
    @CsvSource(value = {
            "{}|true",
            "{\"emailNotificationsEnabled\":false}|false",
            "{\"emailNotificationsEnabled\":true}|true"
    }, delimiter = '|')
    void shouldResolveCurrentEmailNotificationPreference(String config, boolean enabled) {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L)
                .email("user@example.com").config(config).build());

        assertThat(service.getEmailNotificationRecipient(1L).isEmailNotificationsEnabled()).isEqualTo(enabled);
    }

    @Test
    void shouldRejectConfigOperationsWhenUserNoLongerExists() {
        UserConfigParamsBO params = new UserConfigParamsBO(1L, Map.of("emailNotificationsEnabled", false));
        assertThatThrownBy(() -> service.replaceConfig(params))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_EXIT.getCode());
        assertThatThrownBy(() -> service.patchConfig(params))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_EXIT.getCode());
        assertThatThrownBy(() -> service.getConfig(1L))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_EXIT.getCode());
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

        runWithTransactionCommit(() -> service.resetPassword(params));

        ArgumentCaptor<OTPValidateContextBO> validateCaptor = ArgumentCaptor.forClass(OTPValidateContextBO.class);
        verify(emailVerifyService).getValid(validateCaptor.capture());
        assertThat(validateCaptor.getValue().getTarget()).isEqualTo("user@example.com");
        assertThat(validateCaptor.getValue().getCode()).isEqualTo("A1B2C3");
        assertThat(validateCaptor.getValue().getBusinessType()).isEqualTo(EmailVerifyBusinessType.FORGOT_PASSWORD);
        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertPasswordUpdatedAndRevokedTokens(1L);
        verify(sessionService).clearUserSessionCache(1L);
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
        verify(sessionService, never()).clearUserSessionCache(any());
        verify(tokenBlacklistService, never()).blacklistAllUserTokens(any(), any(), any());
    }

    @Test
    void shouldChangePasswordWhenOldPasswordMatches() {
        User user = User.builder().id(1L).password("old-hash").build();
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("123456", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        runWithTransactionCommit(() -> service.changePassword(UserChangePasswordParamsBO.builder()
                .userId(1L).oldPassword("123456").newPassword("new-password").build()));

        assertThat(user.getPassword()).isEqualTo("new-hash");
        assertPasswordUpdatedAndRevokedTokens(1L);
        verify(sessionService).clearUserSessionCache(1L);
        verify(otpServiceFactory, never()).get(any());
    }

    /** 登录缓存必须等事务提交后再清，否则并发请求会用旧数据把缓存填回。 */
    @Test
    void shouldClearLoginCacheOnlyAfterPasswordChangeCommit() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).password("old-hash").build());
        when(passwordEncoder.matches("123456", "old-hash")).thenReturn(true);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        TransactionSynchronizationManager.initSynchronization();
        try {
            service.changePassword(UserChangePasswordParamsBO.builder()
                    .userId(1L).oldPassword("123456").newPassword("new-password").build());

            verify(sessionService, never()).clearUserSessionCache(anyLong());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(sessionService).clearUserSessionCache(1L);
            verify(tokenBlacklistService).blacklistAllUserTokens(TokenType.User, 1L, "密码变更");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void shouldRejectPasswordChangeWhenUserDoesNotExist() {
        when(userMapper.selectById(1L)).thenReturn(null);

        assertThatThrownBy(() -> service.changePassword(UserChangePasswordParamsBO.builder()
                .userId(1L).oldPassword("123456").newPassword("new-password").build()))
                .isInstanceOf(AppException.class)
                .extracting("code")
                .isEqualTo(ResultCode.USER_NOT_EXIT.getCode());

        verify(sessionService, never()).clearUserSessionCache(any());
        verify(userMapper, never()).updateById(any(User.class));
        verify(tokenBlacklistService, never()).blacklistAllUserTokens(any(), any(), any());
    }

    @Test
    void shouldRejectPasswordChangeWhenOldPasswordDoesNotMatch() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).password("old-hash").build());
        when(passwordEncoder.matches("wrong-password", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.changePassword(UserChangePasswordParamsBO.builder()
                .userId(1L).oldPassword("wrong-password").newPassword("new-password").build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("原密码错误")
                .extracting("code")
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(passwordEncoder, never()).encode(anyString());
        verify(userMapper, never()).updateById(any(User.class));
        verify(sessionService, never()).clearUserSessionCache(any());
        verify(tokenBlacklistService, never()).blacklistAllUserTokens(any(), any(), any());
    }

    @Test
    void shouldBindEmailAndConsumeCodeWithoutRevokingRefreshSessions() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).build());
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class)))
                .thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.update(isNull(), any())).thenReturn(1);

        runWithTransactionCommit(() -> service.bindEmail(bindEmailParams()));

        ArgumentCaptor<OTPValidateContextBO> validateCaptor = ArgumentCaptor.forClass(OTPValidateContextBO.class);
        verify(emailVerifyService).getValid(validateCaptor.capture());
        assertThat(validateCaptor.getValue().getBusinessType()).isEqualTo(EmailVerifyBusinessType.BIND_EMAIL);
        assertThat(validateCaptor.getValue().getTarget()).isEqualTo("bind@example.com");
        assertThat(validateCaptor.getValue().getCode()).isEqualTo("A1B2C3");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<User>> updateCaptor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userMapper).update(isNull(), updateCaptor.capture());
        assertThat(updateCaptor.getValue().getSqlSet()).contains("email");
        verify(emailVerifyService).use(argThat(context -> context.getOtpId().equals(10L)));
        verify(sessionService).clearUserSessionCache(1L);
        verify(sessionService, never()).revokeRefreshSessions(any());
        verify(tokenBlacklistService, never()).blacklistAllUserTokens(any(), any(), any());
    }

    @Test
    void shouldRejectBindEmailWhenCurrentEmailIsNotBlankWithoutConsumingCode() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).email("kept@example.com").build());

        assertThatThrownBy(() -> service.bindEmail(bindEmailParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已绑定")
                .extracting("code")
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(otpServiceFactory, never()).get(any());
        verify(emailVerifyService, never()).use(any());
        verify(userMapper, never()).update(any(), any());
        verify(sessionService, never()).revokeRefreshSessions(any());
        verify(sessionService, never()).clearUserSessionCache(any());
    }

    @Test
    void shouldRejectBindEmailWhenUpdateMatchesNoRow() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).build());
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class)))
                .thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.bindEmail(bindEmailParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已绑定")
                .extracting("code")
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(emailVerifyService, never()).use(any());
        verify(sessionService, never()).clearUserSessionCache(any());
        verify(sessionService, never()).revokeRefreshSessions(any());
    }

    @Test
    void shouldRejectBindEmailWhenEmailUniqueIndexConflicts() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).build());
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class)))
                .thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.update(isNull(), any()))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("uk_email"));

        assertThatThrownBy(() -> service.bindEmail(bindEmailParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已存在")
                .extracting("code")
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(emailVerifyService, never()).use(any());
        verify(sessionService, never()).revokeRefreshSessions(any());
    }


    @Test
    void shouldFailRegisterWhenUsernameAlreadyExists() {
        stubRegisterValidation();
        when(userMapper.selectOne(any())).thenReturn(User.builder().id(1L).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("用户名已存在");
    }

    @Test
    void shouldFailRegisterWhenEmailAlreadyExists() {
        stubRegisterValidation();
        when(userMapper.selectOne(any())).thenReturn(null, User.builder().id(1L).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邮箱已存在");
    }

    @Test
    void shouldFailRegisterWhenJoinInviteDoesNotExist() {
        stubRegisterValidation();
        when(organizationInviteCodeService.getValid(any(), any()))
                .thenThrow(new AppException(ResultCode.OPERATION_FAIL, "邀请码无效"));

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码无效");
    }

    /** 目标组织不存在时不写入申请，也不消耗验证码。 */
    @Test
    void shouldRejectRegistrationForMissingOrganization() {
        stubRegisterValidation();
        stubJoinInvite();

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(groupApplicationMapper);
        verify(emailVerifyService, never()).use(any());
        verify(organizationInviteCodeService, never()).accept(any());
    }

    /** 禁用组织不得接受注册申请，也不能消耗验证码。 */
    @Test
    void shouldRejectRegistrationForDisabledOrganization() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verifyNoInteractions(groupApplicationMapper);
        verify(emailVerifyService, never()).use(any());
        verify(organizationInviteCodeService, never()).accept(any());
    }


    @Test
    void shouldDeprecatePreviousAvatarWhenReplacingIt() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).avatarFileId(10L).build());
        UserUpdateMyInfoParamsBO params = UserUpdateMyInfoParamsBO.builder()
                .userId(1L).nickname("user").avatarFileId(20L).build();

        service.updateMyInfo(params);

        verify(resourceService).bindBizResource(argThat(resource -> resource.getResourceId().equals(20L)
                && resource.getReplacedResourceId().equals(10L)
                && resource.getBizId().equals(1L)
                && "avatar".equals(resource.getBizType())
                && resource.getOrgId() == null));
    }

    @Test
    void shouldKeepCurrentAvatarWhenFileIdIsUnchanged() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).avatarFileId(10L).build());
        UserUpdateMyInfoParamsBO params = UserUpdateMyInfoParamsBO.builder()
                .userId(1L).nickname("user").avatarFileId(10L).build();

        service.updateMyInfo(params);
        verify(resourceService).bindBizResource(argThat(resource -> resource.getResourceId().equals(10L)
                && resource.getReplacedResourceId().equals(10L)));
    }

    @Test
    void registerAndJoinOrganizationStoresInviteIdOnApplication() {
        UserRegisterByInviteParamsBO params = UserRegisterByInviteParamsBO.builder()
                .inviteCode("JOIN01").username("newuser").password("password")
                .nickname("New User").email("new@example.com").code("EMAIL1")
                .build();
        OneTimePassword emailOtp = OneTimePassword.builder().id(10L).build();
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class))).thenReturn(emailOtp);
        when(organizationInviteCodeService.getValid(any(), any()))
                .thenReturn(OrganizationInviteCode.builder().id(20L).orgId(30L).build());
        when(userMapper.selectOne(any())).thenReturn(null);
        when(groupApplicationMapper.selectOne(any())).thenReturn(null);
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.ENABLED).build());
        when(passwordEncoder.encode("password")).thenReturn("hashed-password");
        doAnswer(invocation -> {
            invocation.getArgument(0, online.longlian.app.pojo.entity.GroupApplication.class).setId(123L);
            return 1;
        }).when(groupApplicationMapper).insert(any(GroupApplication.class));

        service.registerAndJoinOrganizationByInvite(params);

        ArgumentCaptor<GroupApplication> applicationCaptor = ArgumentCaptor.forClass(GroupApplication.class);
        verify(groupApplicationMapper).insert(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getOtpId()).isEqualTo(20L);
        assertThat(applicationCaptor.getValue().getUserId()).isNull();
        assertThat(applicationCaptor.getValue().getPasswordHash()).isEqualTo("hashed-password");
        verify(userMapper, never()).insert(any(User.class));
        verify(organizationInviteCodeService).accept(argThat(accepted ->
                Long.valueOf(20L).equals(accepted.getId()) && accepted.getInviteeUserId() == null));
        verify(emailVerifyService).use(argThat(context -> context.getOtpId().equals(10L)));
    }
    @Test
    void shouldNotUpdateDefaultOrgWhenMembershipRejected() {
        when(organizationMembershipService.requireEnabledMember(1L, 2L))
                .thenThrow(new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员"));

        assertThatThrownBy(() -> service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("您不是该组织成员");

        verify(userMapper, never()).update(any(), any());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldUpdateDefaultOrgOnceWhenMembershipEnabled() {
        when(organizationMembershipService.requireEnabledMember(1L, 2L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(2L).userId(1L).orgRole("ORG_ADMIN").status(Status.ENABLED).build());
        when(organizationMapper.selectById(2L)).thenReturn(Organization.builder().id(2L).name("组织2").build());

        UserSwitchOrgResultBO result = service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build());

        assertThat(result.getId()).isEqualTo(2L);
        assertThat(result.getRole()).isEqualTo("ORG_ADMIN");
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<User>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userMapper).update(isNull(), captor.capture());
        String sqlSet = captor.getValue().getSqlSet();
        if (sqlSet != null) {
            assertThat(sqlSet).contains("default_org_id");
        }
        verifyNoInteractions(sessionService);
    }
    @Test
    void shouldReturnNoRoleWhenMembershipRoleIsBlank() {
        when(organizationMembershipService.requireEnabledMember(1L, 2L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(2L).userId(1L).orgRole(" ").status(Status.ENABLED).build());
        when(organizationMapper.selectById(2L)).thenReturn(Organization.builder().id(2L).name("组织2").build());

        UserSwitchOrgResultBO result = service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build());

        assertThat(result.getRole()).isNull();
    }
    @Test
    void shouldRejectMissingUserAndEmptyOrganizationList() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        when(userMapper.selectById(1L)).thenReturn(null);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.getMyInfo(1L))
                .isInstanceOf(AppException.class)
                .hasMessage("用户不存在");
        assertThat(service.getMyOrganizations(1L)).isEmpty();
    }

    /** 组织列表过滤禁用组织，并按资源 ID 匹配头像；没有头像的组织保留空值。 */
    @Test
    void shouldListEnabledOrganizationsWithMatchingAvatars() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        when(organizationMemberMapper.selectList(any())).thenReturn(List.of(
                OrganizationMember.builder().orgId(10L).build(),
                OrganizationMember.builder().orgId(20L).build(),
                OrganizationMember.builder().orgId(30L).build()));
        when(organizationMapper.selectBatchIds(any())).thenReturn(List.of(
                Organization.builder().id(10L).name("with avatar").status(Status.ENABLED).avatarFileId(100L).build(),
                Organization.builder().id(20L).name("disabled").status(Status.DISABLED).avatarFileId(200L).build(),
                Organization.builder().id(30L).name("without avatar").status(Status.ENABLED).build()));
        when(resourceService.getResourceReadUrls(List.of(100L))).thenReturn(Map.of(
                100L, new ResourceReadUrlGetResultBO("https://cdn.example/avatar.png", 10L, "avatar.png")));

        assertThat(service.getMyOrganizations(1L))
                .extracting("id", "name", "avatarUrl")
                .containsExactly(
                        org.assertj.core.api.Assertions.tuple(10L, "with avatar",
                                "https://cdn.example/avatar.png?eo-img.resize=w/64/h/64"),
                        org.assertj.core.api.Assertions.tuple(30L, "without avatar", null));
    }

    @Test
    void shouldResizeCurrentUserAvatar() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).username("u").avatarFileId(9L).build());
        when(resourceService.getResourceReadUrl(9L))
                .thenReturn("https://cdn.example/avatar.png?token=abc&t=1");

        UserGetMyInfoResultBO info = service.getMyInfo(1L);

        assertThat(info.getAvatarUrl())
                .isEqualTo("https://cdn.example/avatar.png?token=abc&t=1&eo-img.resize=w/64/h/64");
    }

    @Test
    void shouldIncludeAvatarWhenSwitchingToOrganizationWithAvatar() {
        when(organizationMembershipService.requireEnabledMember(1L, 2L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(2L).userId(1L).orgRole("ORG_ADMIN").status(Status.ENABLED).build());
        when(organizationMapper.selectById(2L)).thenReturn(
                Organization.builder().id(2L).name("组织2").avatarFileId(5L).build());
        when(resourceService.getResourceReadUrl(5L)).thenReturn("http://avatar");

        UserSwitchOrgResultBO result = service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build());

        assertThat(result.getAvatarUrl()).isEqualTo("http://avatar");
    }

    @Test
    void shouldRejectCreateOrganizationWhenNameIsBlank() {
        stubRegisterValidation();
        when(organizationInviteCodeService.getValid(any(), any()))
                .thenReturn(OrganizationInviteCode.builder().id(20L).build());

        assertThatThrownBy(() -> service.registerAndCreateOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织名称不能为空");
    }

    @Test
    void shouldAssignOwnerWhenRegisteringAndCreatingOrganization() {
        stubRegisterValidation();
        when(organizationInviteCodeService.getValid(any(), any()))
                .thenReturn(OrganizationInviteCode.builder().id(20L).build());
        when(passwordEncoder.encode(any())).thenReturn("hash");
        UserRegisterByInviteParamsBO params = registerParams();
        params.setOrgName("测试组织");

        service.registerAndCreateOrganizationByInvite(params);

        ArgumentCaptor<OrganizationMember> memberCaptor = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(organizationMemberMapper).insert(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getOrgRole()).isEqualTo("ORG_OWNER");
    }

    @Test
    void shouldRejectJoinRegistrationWhenApplicationIsAlreadyPending() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.ENABLED).build());
        when(groupApplicationMapper.selectOne(any())).thenReturn(GroupApplication.builder().id(1L).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("您已提交过入组申请");
    }




    private void assertPasswordUpdatedAndRevokedTokens(long userId) {
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LambdaUpdateWrapper<User>> captor = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(userMapper).update(isNull(), captor.capture());
        assertThat(captor.getValue().getSqlSet()).contains("password");
        verify(userMapper, never()).updateById(any(User.class));
        var order = inOrder(sessionService, tokenBlacklistService);
        order.verify(sessionService).revokeRefreshSessions(userId);
        order.verify(tokenBlacklistService).blacklistAllUserTokens(TokenType.User, userId, "密码变更");
    }

    private void runWithTransactionCommit(Runnable action) {
        TransactionSynchronizationManager.initSynchronization();
        try {
            action.run();
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }


    @Test
    void shouldConvertDuplicateRegistrationInsertToBusinessFailureWithoutConsumingCodes() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.ENABLED).build());
        when(groupApplicationMapper.insert(any(GroupApplication.class)))
                .thenThrow(new org.springframework.dao.DuplicateKeyException("pending registration"));
        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(emailVerifyService, never()).use(any());
        verify(organizationInviteCodeService, never()).accept(any());
        verify(userMapper, never()).insert(any(User.class));
    }

    private void stubRegisterValidation() {
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class))).thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.selectOne(any())).thenReturn(null);
    }

    private void stubJoinInvite() {
        when(organizationInviteCodeService.getValid(any(), any()))
                .thenReturn(OrganizationInviteCode.builder().id(20L).orgId(30L).build());
    }

    private UserRegisterByInviteParamsBO registerParams() {
        return UserRegisterByInviteParamsBO.builder()
                .inviteCode("JOIN01")
                .username("newuser")
                .password("password")
                .nickname("New User")
                .email("new@example.com")
                .code("EMAIL1")
                .build();
    }

    private UserResetPasswordParamsBO resetPasswordParams() {
        return UserResetPasswordParamsBO.builder()
                .email("user@example.com")
                .code("A1B2C3")
                .password("new-password")
                .build();
    }

    private UserBindEmailParamsBO bindEmailParams() {
        return UserBindEmailParamsBO.builder()
                .userId(1L)
                .email("bind@example.com")
                .code("A1B2C3")
                .build();
    }
}

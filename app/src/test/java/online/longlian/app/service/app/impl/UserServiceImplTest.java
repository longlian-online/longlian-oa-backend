package online.longlian.app.service.app.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.app.UserResetPasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserChangePasswordParamsBO;
import online.longlian.app.pojo.bo.app.UserUpdateMyInfoParamsBO;
import online.longlian.app.pojo.bo.common.OTPUseContextBO;
import online.longlian.app.pojo.bo.common.OTPValidateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.bo.app.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.app.UserSwitchOrgParamsBO;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.OrganizationAuthorizationService;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.util.List;

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
    private OrganizationJoinOtpMapper organizationJoinOtpMapper;
    @Mock
    private GroupApplicationMapper groupApplicationMapper;
    @Mock
    private ResourceService resourceService;
    @Mock
    private UserMapper userMapper;
    private OrganizationAuthorizationService organizationAuthorizationService;
    @Mock
    private OTPServiceFactory otpServiceFactory;
    @Mock
    private OTPStrategyService emailVerifyService;
    @Mock
    private OTPStrategyService joinInviteService;
    @Mock
    private SessionService sessionService;
    @Mock
    private TokenBlacklistService tokenBlacklistService;

    private UserServiceImpl service;

    @BeforeEach
    void setUp() {
        for (Class<?> entity : List.of(User.class, Organization.class, OrganizationMember.class))
            TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), entity);
        organizationAuthorizationService = new OrganizationAuthorizationService(
                organizationMapper, organizationMemberMapper, userMapper);
        service = new UserServiceImpl(
                passwordEncoder,
                organizationMapper,
                organizationMemberMapper,
                organizationJoinOtpMapper,
                groupApplicationMapper,
                resourceService,
                userMapper,
                organizationAuthorizationService,
                otpServiceFactory,
                Clock.systemUTC(),
                sessionService,
                tokenBlacklistService
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
        stubJoinInvite();
        when(organizationJoinOtpMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码不存在");
    }

    @Test
    void shouldFailRegisterWhenJoinOrganizationDoesNotExist() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationJoinOtpMapper.selectOne(any())).thenReturn(
                OrganizationJoinOtp.builder().otpId(20L).orgId(30L).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织不存在");
    }

    @Test
    void shouldFailRegisterWhenJoinOrganizationIsDisabled() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationJoinOtpMapper.selectOne(any())).thenReturn(
                OrganizationJoinOtp.builder().otpId(20L).orgId(30L).build());
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.DISABLED).build());

        assertThatThrownBy(() -> service.registerAndJoinOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织已被禁用");
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
        OneTimePassword inviteOtp = OneTimePassword.builder().id(20L).build();
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(otpServiceFactory.get(OTPType.OrganizationUserInvite)).thenReturn(joinInviteService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class))).thenReturn(emailOtp);
        when(joinInviteService.getValid(any(OTPValidateContextBO.class))).thenReturn(inviteOtp);
        when(userMapper.selectOne(any())).thenReturn(null);
        when(groupApplicationMapper.selectCount(any())).thenReturn(0L);
        when(organizationJoinOtpMapper.selectOne(any())).thenReturn(
                OrganizationJoinOtp.builder().otpId(20L).orgId(30L).build());
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.ENABLED).build());
        when(passwordEncoder.encode("password")).thenReturn("hashed-password");
        doAnswer(invocation -> {
            invocation.getArgument(0, User.class).setId(99L);
            return 1;
        }).when(userMapper).insert(any(User.class));
        doAnswer(invocation -> {
            invocation.getArgument(0, online.longlian.app.pojo.entity.GroupApplication.class).setId(123L);
            return 1;
        }).when(groupApplicationMapper).insert(any(GroupApplication.class));

        service.registerAndJoinOrganizationByInvite(params);

        ArgumentCaptor<GroupApplication> applicationCaptor = ArgumentCaptor.forClass(GroupApplication.class);
        verify(groupApplicationMapper).insert(applicationCaptor.capture());
        assertThat(applicationCaptor.getValue().getOtpId()).isEqualTo(20L);
        assertThat(applicationCaptor.getValue().getUserId()).isEqualTo(99L);
        verify(userMapper).insert(argThat((User user) -> user.getStatus() == Status.DISABLED
                && "hashed-password".equals(user.getPassword())));
        verify(joinInviteService).use(argThat(context -> context.getOtpId().equals(20L)
                && context.getUserId() == null));
        verify(emailVerifyService).use(argThat(context -> context.getOtpId().equals(10L)));
    }
    /** 成员已退出或禁用时，不能重新写入失效的默认组织。 */
    @Test
    void shouldNotUpdateDefaultOrgWhenMembershipRejected() {
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(2L).status(Status.ENABLED).build());
        assertThatThrownBy(() -> service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(userMapper, never()).update(any(), any());
    }

    /** 非法角色不能通过默认组织切换进入响应或授权。 */
    @Test
    void shouldRejectInvalidMembershipRoleBeforeUpdatingDefaultOrganization() {
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(2L).status(Status.ENABLED).build());
        when(organizationMemberMapper.selectOne(any())).thenReturn(
                OrganizationMember.builder().id(9L).orgId(2L).userId(1L).orgRole(" ").status(Status.ENABLED).build());
        assertThatThrownBy(() -> service.switchOrg(UserSwitchOrgParamsBO.builder().userId(1L).orgId(2L).build()))
                .isInstanceOf(AppException.class).extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        verify(userMapper, never()).update(any(), any());
    }

    /** 新组织创建事务内建立所有者，不沿用旧版普通管理员默认角色。 */
    @Test
    void shouldCreateOrganizationWithOwnerMembership() {
        stubRegisterValidation();
        when(otpServiceFactory.get(OTPType.OrganizationInvite)).thenReturn(joinInviteService);
        when(joinInviteService.getValid(any())).thenReturn(OneTimePassword.builder().id(20L).build());
        when(passwordEncoder.encode("password")).thenReturn("hash");
        doAnswer(call -> { call.<User>getArgument(0).setId(50L); return 1; }).when(userMapper).insert(any(User.class));
        doAnswer(call -> { call.<Organization>getArgument(0).setId(30L); return 1; })
                .when(organizationMapper).insert(any(Organization.class));
        UserRegisterByInviteParamsBO params = registerParams();
        params.setOrgName("Owner Org");

        service.registerAndCreateOrganizationByInvite(params);

        ArgumentCaptor<OrganizationMember> created = ArgumentCaptor.forClass(OrganizationMember.class);
        verify(organizationMemberMapper).insert(created.capture());
        assertThat(created.getValue().getOrgRole()).isEqualTo("ORG_OWNER");
        assertThat(created.getValue().getOrgId()).isEqualTo(30L);
        assertThat(created.getValue().getUserId()).isEqualTo(50L);
    }

    /** 组织头像不可用不影响全局个人资料读取。 */
    @Test
    void shouldReturnProfileWithoutUnavailableAvatar() {
        when(userMapper.selectById(1L)).thenReturn(User.builder().id(1L).username("user")
                .email("user@example.com").avatarFileId(10L).build());
        when(resourceService.getResourceReadUrls(List.of(10L))).thenReturn(java.util.Map.of());

        var profile = service.getMyInfo(1L);

        assertThat(profile.getUsername()).isEqualTo("user");
        assertThat(profile.getEmail()).isEqualTo("user@example.com");
        assertThat(profile.getAvatarUrl()).isNull();
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


    @Test
    void shouldRejectCreateOrganizationWhenNameIsBlank() {
        stubRegisterValidation();
        when(otpServiceFactory.get(OTPType.OrganizationInvite)).thenReturn(joinInviteService);
        when(joinInviteService.getValid(any(OTPValidateContextBO.class))).thenReturn(OneTimePassword.builder().id(20L).build());

        assertThatThrownBy(() -> service.registerAndCreateOrganizationByInvite(registerParams()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织名称不能为空");
    }

    @Test
    void shouldRejectJoinRegistrationWhenApplicationIsAlreadyPending() {
        stubRegisterValidation();
        stubJoinInvite();
        when(organizationJoinOtpMapper.selectOne(any())).thenReturn(
                OrganizationJoinOtp.builder().otpId(20L).orgId(30L).build());
        when(organizationMapper.selectOne(any())).thenReturn(
                Organization.builder().id(30L).status(Status.ENABLED).build());
        when(groupApplicationMapper.selectCount(any())).thenReturn(1L);

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
        verify(tokenBlacklistService).blacklistAllUserTokens(TokenType.User, userId, "密码变更");
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

    private void stubRegisterValidation() {
        when(otpServiceFactory.get(OTPType.EmailVerify)).thenReturn(emailVerifyService);
        when(emailVerifyService.getValid(any(OTPValidateContextBO.class))).thenReturn(OneTimePassword.builder().id(10L).build());
        when(userMapper.selectOne(any())).thenReturn(null);
    }

    private void stubJoinInvite() {
        when(otpServiceFactory.get(OTPType.OrganizationUserInvite)).thenReturn(joinInviteService);
        when(joinInviteService.getValid(any(OTPValidateContextBO.class))).thenReturn(OneTimePassword.builder().id(20L).build());
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
}

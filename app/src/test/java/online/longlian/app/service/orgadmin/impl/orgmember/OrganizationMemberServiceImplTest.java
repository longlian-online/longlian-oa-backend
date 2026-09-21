package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberResetPasswordParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberResetPasswordResultBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceImplTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC);
    @Mock private GroupApplicationMapper groupApplicationMapper;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private OTPServiceFactory otpServiceFactory;
    @Mock private UserMapper userMapper;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private MemberQueryBuilder memberQueryBuilder;
    @Mock private MemberAssembler memberAssembler;
    @Mock private ApplicationReviewHandler applicationReviewHandler;
    @Mock private MemberStatusHandler memberStatusHandler;
    @Mock private MemberSubmissionHandler memberSubmissionHandler;
    @Mock private LockService lockService;
    @Mock private SessionService sessionService;

    private OrganizationMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        service = new OrganizationMemberServiceImpl(
                clock, groupApplicationMapper, organizationMemberMapper, otpServiceFactory,
                userMapper, passwordEncoder, memberQueryBuilder, memberAssembler,
                applicationReviewHandler, memberStatusHandler, memberSubmissionHandler,
                lockService, sessionService);
    }

    @Test
    void shouldUpdateRoleWithinOrganizationAndClearSession() {
        OrganizationMember member = member(InviteConstants.ROLE_ORG_USER, Status.ENABLED);
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member);

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_ADMIN));

        verify(organizationMemberMapper).update(eq(null), any());
        verify(lockService, never()).tryAcquireOrThrow(anyString(), anyLong(), anyLong(), any(TimeUnit.class));
        verify(sessionService).clearUserSessionCache(20L);
    }

    @Test
    void shouldRejectLastEnabledAdminDowngradeAndReleaseLock() {
        OrganizationMember member = member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED);
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member);
        when(lockService.tryAcquireOrThrow("org:member:role:1", 0, 5, TimeUnit.SECONDS)).thenReturn(lock);
        when(organizationMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织至少保留一名管理员")
                .extracting("code")
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(lock).close();
        verify(organizationMemberMapper, never()).update(eq(null), any());
        verify(sessionService, never()).clearUserSessionCache(anyLong());
    }

    @Test
    void shouldEncodeGeneratedPasswordWithoutPersistingPlaintext() {
        OrganizationMember member = member(InviteConstants.ROLE_ORG_USER, Status.DISABLED);
        User user = User.builder().id(20L).password("old-hash").build();
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member);
        when(userMapper.selectById(20L)).thenReturn(user);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        OrgMemberResetPasswordResultBO result = service.resetMemberPassword(
                OrgMemberResetPasswordParamsBO.builder().orgId(1L).memberId(2L).build());

        ArgumentCaptor<String> passwordCaptor = ArgumentCaptor.forClass(String.class);
        verify(passwordEncoder).encode(passwordCaptor.capture());
        assertThat(passwordCaptor.getValue()).matches("[A-Za-z0-9]{12}").isEqualTo(result.getPassword());
        assertThat(user.getPassword()).isEqualTo("encoded-password").doesNotContain(result.getPassword());
        verify(userMapper).updateById(user);
        verify(sessionService).revokeUserSessions(20L, "管理员重置成员密码");
        assertThat(member.getStatus()).isEqualTo(Status.DISABLED);
    }

    private OrganizationMember member(String role, Status status) {
        return OrganizationMember.builder().id(2L).orgId(1L).userId(20L).orgRole(role).status(status).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(String role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).memberId(2L).orgRole(role).build();
    }
}

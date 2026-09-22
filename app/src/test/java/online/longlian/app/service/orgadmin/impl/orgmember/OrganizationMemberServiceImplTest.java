package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

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
                memberQueryBuilder, memberAssembler,
                applicationReviewHandler, memberStatusHandler, memberSubmissionHandler,
                lockService, sessionService);
    }

    @Test
    void shouldClearRoleSessionOnlyAfterCommit() {
        OrganizationMember member = member(InviteConstants.ROLE_ORG_USER, Status.ENABLED);
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_ADMIN));

            verify(sessionService, never()).clearUserSessionCache(anyLong());
            TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
            verify(sessionService).clearUserSessionCache(20L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
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
        when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);
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
    void shouldKeepRoleLockUntilTheTransactionCompletes() {
        OrganizationMember member = member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED);
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member);
        when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);
        when(organizationMemberMapper.selectCount(any())).thenReturn(2L);
        TransactionSynchronizationManager.initSynchronization();
        try {
            service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

            verify(lock, never()).close();
            verify(organizationMemberMapper).update(eq(null), any());
            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(sync -> sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
            verify(lock).close();
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * 等锁期间成员已被并发请求降级，复核后不再是管理员，
     * 此时直接更新即可，不再统计管理员数量。
     */
    @Test
    void shouldSkipAdminCountCheckWhenMemberIsNoLongerAdminAfterLock() {
        OrganizationMember admin = member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED);
        OrganizationMember demoted = member(InviteConstants.ROLE_ORG_USER, Status.ENABLED);
        DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(admin).thenReturn(demoted);
        when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

        verify(organizationMemberMapper, never()).selectCount(any());
        verify(organizationMemberMapper).update(eq(null), any());
        verify(lock).close();
        verify(sessionService).clearUserSessionCache(20L);
    }

    private OrganizationMember member(String role, Status status) {
        return OrganizationMember.builder().id(2L).orgId(1L).userId(20L).orgRole(role).status(status).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(String role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).memberId(2L).orgRole(role).build();
    }
}

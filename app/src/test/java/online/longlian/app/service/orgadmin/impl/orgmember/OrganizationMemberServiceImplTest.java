package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
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
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
    private RecordingTransactionManager transactions;
    private DistributedLockService.Lock lock;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        transactions = new RecordingTransactionManager();
        lock = mock(DistributedLockService.Lock.class);
        lenient().when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);
        lenient().when(organizationMemberMapper.selectOne(any())).thenReturn(operator(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));
        service = new OrganizationMemberServiceImpl(clock, groupApplicationMapper, organizationMemberMapper,
                otpServiceFactory, memberQueryBuilder, memberAssembler, applicationReviewHandler,
                memberStatusHandler, memberSubmissionHandler, lockService, sessionService, transactions);
    }

    @Test
    void shouldLockBeforeTransactionAndClearSessionOnlyAfterCommit() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED));
        transactions.onBegin = () -> verify(lockService).tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS);
        transactions.onCommit = () -> {
            verify(lock, never()).close();
            verify(sessionService, never()).clearUserSessionCache(anyLong());
        };

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_ADMIN));

        InOrder order = inOrder(lockService, organizationMemberMapper, memberStatusHandler, lock);
        order.verify(lockService).tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS);
        order.verify(organizationMemberMapper).selectOne(any());
        order.verify(memberStatusHandler).getAndValidateMember(2L, 1L);
        order.verify(lock).close();
        assertThat(transactions.propagation).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(sessionService).clearUserSessionCache(20L);
        verify(organizationMemberMapper).update(eq(null), any());
    }

    @Test
    void shouldRejectLastEnabledAdminAndRollback() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));
        when(organizationMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织至少保留一名管理员")
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());

        verify(lock).close();
        verify(organizationMemberMapper, never()).update(eq(null), any());
        verify(sessionService, never()).clearUserSessionCache(anyLong());
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    void shouldAllowDowngradeWhenAnotherEnabledAdminRemains() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));
        when(organizationMemberMapper.selectCount(any())).thenReturn(2L);

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

        verify(organizationMemberMapper).update(eq(null), any());
        verify(lock).close();
        verify(sessionService).clearUserSessionCache(20L);
    }

    @Test
    void shouldCheckLatestRoleEvenWhenTargetWasPromotedBeforeLock() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));
        when(organizationMemberMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("组织至少保留一名管理员");

        verify(organizationMemberMapper, never()).update(eq(null), any());
    }

    @Test
    void shouldSkipAdminCountForOrdinaryOrDisabledMember() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L))
                .thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED))
                .thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.DISABLED));

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));
        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

        verify(organizationMemberMapper, never()).selectCount(any());
        verify(lock, times(2)).close();
    }

    @Test
    void shouldRejectDemotedOperatorBeforeReadingTarget() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(operator(InviteConstants.ROLE_ORG_USER, Status.ENABLED));

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_ADMIN)))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());

        verify(memberStatusHandler, never()).getAndValidateMember(anyLong(), anyLong());
        verify(organizationMemberMapper, never()).update(eq(null), any());
        verify(lock).close();
    }

    @Test
    void shouldRejectDisabledOrMissingOperator() {
        when(organizationMemberMapper.selectOne(any()))
                .thenReturn(operator(InviteConstants.ROLE_ORG_ADMIN, Status.DISABLED))
                .thenReturn(null);

        assertThatThrownBy(() -> service.changeMemberStatus(statusParams(Status.DISABLED)))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());

        verify(memberStatusHandler, never()).updateMemberStatus(any(), any());
        verify(lock, times(2)).close();
    }

    @Test
    void shouldRejectDisablingMemberPromotedBeforeStatusMutation() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));
        doThrow(new AppException(ResultCode.OPERATION_FAIL, "管理员不可被禁用"))
                .when(memberStatusHandler).validateNotAdminDisable(any(), eq(Status.DISABLED));

        assertThatThrownBy(() -> service.changeMemberStatus(statusParams(Status.DISABLED)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("管理员不可被禁用");

        verify(memberStatusHandler, never()).updateMemberStatus(any(), any());
        verify(lock).close();
    }

    @Test
    void shouldClearDisabledMemberSessionOnlyAfterCommit() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED));
        transactions.onCommit = () -> verify(sessionService, never()).clearUserSessionCache(anyLong());

        service.changeMemberStatus(statusParams(Status.DISABLED));

        verify(memberStatusHandler).updateMemberStatus(any(), eq(Status.DISABLED));
        verify(sessionService).clearUserSessionCache(20L);
        verify(lock).close();
    }

    private OrganizationMember member(String role, Status status) {
        return OrganizationMember.builder().id(2L).orgId(1L).userId(20L).orgRole(role).status(status).build();
    }

    private OrganizationMember operator(String role, Status status) {
        return OrganizationMember.builder().id(1L).orgId(1L).userId(10L).orgRole(role).status(status).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(String role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).orgRole(role).build();
    }

    private OrgMemberChangeStatusParamsBO statusParams(Status status) {
        return OrgMemberChangeStatusParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).status(status).build();
    }

    private static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private Runnable onBegin = () -> {};
        private Runnable onCommit = () -> {};
        private int propagation;
        private int rollbacks;

        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) {
            propagation = definition.getPropagationBehavior();
            onBegin.run();
        }
        @Override protected void doCommit(DefaultTransactionStatus status) { onCommit.run(); }
        @Override protected void doRollback(DefaultTransactionStatus status) { rollbacks++; }
    }
}

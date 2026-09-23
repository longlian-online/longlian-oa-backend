package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberTransferOwnershipParamsBO;
import online.longlian.app.pojo.entity.Organization;
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
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceImplTest {
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC);
    @Mock private GroupApplicationMapper groupApplicationMapper;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private UserMapper userMapper;
    @Mock private OTPServiceFactory otpServiceFactory;
    @Mock private MemberQueryBuilder memberQueryBuilder;
    @Mock private MemberAssembler memberAssembler;
    @Mock private ApplicationReviewHandler applicationReviewHandler;
    @Mock private MemberStatusHandler memberStatusHandler;
    @Mock private MemberSubmissionHandler memberSubmissionHandler;
    @Mock private LockService lockService;
    @Mock private SessionService sessionService;

    private OrganizationMemberServiceImpl service;
    private DistributedLockService.Lock lock;

    @BeforeEach
    void setUp() {
        MybatisConfiguration configuration = new MybatisConfiguration();
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), Organization.class);
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(configuration, ""), OrganizationMember.class);
        lock = mock(DistributedLockService.Lock.class);
        when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);
        service = new OrganizationMemberServiceImpl(clock, groupApplicationMapper, organizationMapper,
                organizationMemberMapper, userMapper, otpServiceFactory, memberQueryBuilder, memberAssembler,
                applicationReviewHandler, memberStatusHandler, memberSubmissionHandler, lockService, sessionService,
                new ImmediateTransactionManager());
    }

    @Test
    void shouldAllowOwnerToPromoteAndDemoteAdministrator() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(owner());
        when(organizationMapper.selectById(1L)).thenReturn(organization(10L));
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(20L, InviteConstants.ROLE_ORG_ADMIN));

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

        verify(organizationMemberMapper).update(eq(null), any());
        verify(sessionService).clearUserSessionCache(20L);
    }

    @Test
    void shouldRejectAdministratorManagingPeerOrOwner() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(10L, InviteConstants.ROLE_ORG_ADMIN));
        when(memberStatusHandler.getAndValidateMember(2L, 1L))
                .thenReturn(member(20L, InviteConstants.ROLE_ORG_ADMIN))
                .thenReturn(member(30L, InviteConstants.ROLE_ORG_OWNER));

        assertThatThrownBy(() -> service.changeMemberStatus(statusParams(Status.DISABLED)))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.changeMemberStatus(statusParams(Status.DISABLED)))
                .isInstanceOf(AppException.class);

        verify(memberStatusHandler, never()).updateMemberStatus(any(), any());
        verifyNoInteractions(sessionService);
    }

    @Test
    void shouldAllowAdministratorToManageOrdinaryMember() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(member(10L, InviteConstants.ROLE_ORG_ADMIN));
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(20L, InviteConstants.ROLE_ORG_USER));

        service.changeMemberStatus(statusParams(Status.DISABLED));

        verify(memberStatusHandler).updateMemberStatus(any(), eq(Status.DISABLED));
        verify(sessionService).clearUserSessionCache(20L);
    }

    @Test
    void shouldRejectOwnerDisablingThemself() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(owner());
        when(memberStatusHandler.getAndValidateMember(1L, 1L)).thenReturn(owner());

        assertThatThrownBy(() -> service.changeMemberStatus(OrgMemberChangeStatusParamsBO.builder()
                .orgId(1L).operatorUserId(10L).memberId(1L).status(Status.DISABLED).build()))
                .isInstanceOf(AppException.class);

        verify(memberStatusHandler, never()).updateMemberStatus(any(), any());
    }

    @Test
    void shouldTransferOwnershipAndRevokeBothSessions() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(owner());
        when(organizationMapper.selectById(1L)).thenReturn(organization(10L));
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(20L, InviteConstants.ROLE_ORG_ADMIN));
        when(organizationMapper.update(eq(null), any())).thenReturn(1);

        service.transferOwnership(OrgMemberTransferOwnershipParamsBO.builder()
                .orgId(1L).operatorUserId(10L).memberId(2L).build());

        verify(organizationMapper).update(eq(null), any());
        verify(organizationMemberMapper, times(2)).update(eq(null), any());
        verify(sessionService).clearUserSessionCache(10L);
        verify(sessionService).clearUserSessionCache(20L);
    }

    private Organization organization(Long ownerUserId) {
        return Organization.builder().id(1L).ownerUserId(ownerUserId).status(Status.ENABLED).build();
    }

    private OrganizationMember owner() {
        return OrganizationMember.builder().id(1L).orgId(1L).userId(10L)
                .orgRole(InviteConstants.ROLE_ORG_OWNER).status(Status.ENABLED).build();
    }

    private OrganizationMember member(Long userId, String role) {
        return OrganizationMember.builder().id(2L).orgId(1L).userId(userId).orgRole(role).status(Status.ENABLED).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(String role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).orgRole(role).build();
    }

    private OrgMemberChangeStatusParamsBO statusParams(Status status) {
        return OrgMemberChangeStatusParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).status(status).build();
    }

    private static class ImmediateTransactionManager extends AbstractPlatformTransactionManager {
        @Override protected Object doGetTransaction() { return new Object(); }
        @Override protected void doBegin(Object transaction, TransactionDefinition definition) { }
        @Override protected void doCommit(DefaultTransactionStatus status) { }
        @Override protected void doRollback(DefaultTransactionStatus status) { }
    }
}

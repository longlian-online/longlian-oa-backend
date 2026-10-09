package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.orgadmin.*;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.common.*;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import online.longlian.common.enumeration.ApplicationStatus;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceImplTest {
    private final Clock clock=Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"),ZoneOffset.UTC);
    @Mock private GroupApplicationMapper groupApplicationMapper;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private OTPServiceFactory otpServiceFactory;
    @Mock private MemberQueryBuilder memberQueryBuilder;
    @Mock private MemberAssembler memberAssembler;
    @Mock private ApplicationReviewHandler applicationReviewHandler;
    @Mock private MemberStatusHandler memberStatusHandler;
    @Mock private MemberSubmissionHandler memberSubmissionHandler;
    @Mock private LockService lockService;
    @Mock private OrganizationAuthorizationService authorization;
    private OrganizationMemberServiceImpl service;
    private RecordingTransactionManager transactions;
    private DistributedLockService.Lock lock;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(),""),OrganizationMember.class);
        transactions=new RecordingTransactionManager();
        lenient().when(organizationMemberMapper.update(isNull(),any())).thenReturn(1);
        lock=mock(DistributedLockService.Lock.class);
        lenient().when(lockService.tryAcquireOrThrow("org:member:role:1",0,TimeUnit.SECONDS)).thenReturn(lock);
        lenient().when(authorization.requireManager(1L,10L)).thenReturn(operator("ORG_OWNER",Status.ENABLED));
        service=new OrganizationMemberServiceImpl(clock,groupApplicationMapper,organizationMemberMapper,otpServiceFactory,
                memberQueryBuilder,memberAssembler,applicationReviewHandler,memberStatusHandler,memberSubmissionHandler,
                lockService,transactions,authorization,new OrganizationMemberPolicy());
    }

    @Test
    void shouldLockBeforeTransactionAndAuthorizeBeforeMutation() {
        when(memberStatusHandler.getAndValidateMember(2L,1L)).thenReturn(member("ORG_USER",Status.ENABLED));
        service.changeMemberRole(roleParams("ORG_ADMIN"));
        InOrder order=inOrder(lockService,authorization,memberStatusHandler,organizationMemberMapper,lock);
        order.verify(lockService).tryAcquireOrThrow("org:member:role:1",0,TimeUnit.SECONDS);
        order.verify(authorization).lockOrganization(1L,true);
        order.verify(authorization).requireManager(1L,10L);
        order.verify(memberStatusHandler).getAndValidateMember(2L,1L);
        order.verify(organizationMemberMapper).update(isNull(),any());
        order.verify(lock).close();
        assertThat(transactions.propagation).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    @Test
    void shouldRejectAdministratorChangingPeerRoleAndRollback() {
        when(authorization.requireManager(1L,10L)).thenReturn(operator("ORG_ADMIN",Status.ENABLED));
        when(memberStatusHandler.getAndValidateMember(2L,1L)).thenReturn(member("ORG_ADMIN",Status.ENABLED));
        assertThatThrownBy(()->service.changeMemberRole(roleParams("ORG_USER"))).isInstanceOf(AppException.class);
        verify(organizationMemberMapper,never()).update(any(),any());
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    void shouldAllowOwnerToDisableAdministrator() {
        when(memberStatusHandler.getAndValidateMember(2L,1L)).thenReturn(member("ORG_ADMIN",Status.ENABLED));
        service.changeMemberStatus(statusParams(Status.DISABLED));
        verify(memberStatusHandler).updateMemberStatus(any(),eq(Status.DISABLED));
    }

    @Test
    void shouldRejectRevokedOperatorBeforeReadingTarget() {
        when(authorization.requireManager(1L,10L)).thenThrow(new AppException(online.longlian.app.common.result.ResultCode.UNAUTHORIZED_OPERATION));
        assertThatThrownBy(()->service.changeMemberStatus(statusParams(Status.DISABLED))).isInstanceOf(AppException.class);
        verifyNoInteractions(memberStatusHandler);
        assertThat(transactions.rollbacks).isEqualTo(1);
    }

    @Test
    void shouldAssembleNonEmptyApplicationPageAndEmptyMemberPage() {
        Page<GroupApplication> applications = new Page<>(1, 10);
        applications.setRecords(List.of(new GroupApplication()));
        applications.setTotal(1);
        when(memberQueryBuilder.buildApplicationListQuery(any())).thenReturn(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        when(groupApplicationMapper.selectPage(any(), any())).thenReturn(applications);
        when(memberAssembler.assembleApplications(any())).thenReturn(List.of());

        assertThat(service.listApplications(OrgAdminApplicationListParamsBO.builder()
                .orgId(1L).page(new PageParamsBO(1, 10)).build()).getTotal()).isEqualTo(1L);

        Page<OrganizationMember> members = new Page<>(1, 10);
        members.setRecords(List.of());
        members.setTotal(0);
        when(memberQueryBuilder.buildMemberListQuery(any())).thenReturn(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>());
        when(organizationMemberMapper.selectPage(any(), any())).thenReturn(members);

        assertThat(service.listMembers(OrgMemberListParamsBO.builder()
                .orgId(1L).page(new PageParamsBO(1, 10)).build()).getList()).isEmpty();
    }

    @Test
    void shouldRejectMissingOrForeignMemberSubmitCounts() {
        when(organizationMemberMapper.selectById(2L)).thenReturn(null).thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED));

        assertThatThrownBy(() -> service.getMemberBaseTaskSubmitCounts(
                OrgMemberBaseTaskSubmitCountParamsBO.builder().memberId(2L).orgId(1L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("成员不存在");
        assertThatThrownBy(() -> service.getMemberBaseTaskSubmitCounts(
                OrgMemberBaseTaskSubmitCountParamsBO.builder().memberId(2L).orgId(9L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权操作该成员");
    }

    /** 唯一键冲突应返回业务失败，而不是暴露数据库异常。 */
    @Test
    void shouldReportReviewIdentityConflictAsBusinessFailure() {
        when(groupApplicationMapper.selectOne(any())).thenThrow(new DuplicateKeyException("duplicate identity"));

        assertThatThrownBy(() -> service.reviewApplication(OrgAdminReviewApplicationParamsBO.builder()
                .applicationId(1L).orgId(1L).reviewerId(10L)
                .applicationStatus(ApplicationStatus.APPROVED).build()))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.OPERATION_FAIL.getCode());
    }


    private OrganizationMember member(String role, Status status) {
        return OrganizationMember.builder().id(2L).orgId(1L).userId(20L).orgRole(role).status(status).build();
    }

    private OrganizationMember operator(String role, Status status) {
        return OrganizationMember.builder().id(1L).orgId(1L).userId(10L).orgRole(role).status(status).build();
    }

    private OrgMemberChangeRoleParamsBO roleParams(String role) {
        return OrgMemberChangeRoleParamsBO.builder().orgId(1L).operatorUserId(10L).memberId(2L).orgRole(online.longlian.app.common.enumeration.OrganizationRole.fromValue(role)).build();
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

package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OneTimePasswordMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.common.PageParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminJoinInviteCodeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminJoinInviteCodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminRevokeJoinInviteCodeParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.OTPStatus;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import online.longlian.common.enumeration.ApplicationStatus;
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
import org.springframework.dao.DuplicateKeyException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationMemberServiceImplTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC);
    @Mock private GroupApplicationMapper groupApplicationMapper;
    @Mock private OrganizationMemberMapper organizationMemberMapper;
    @Mock private OrganizationJoinOtpMapper organizationJoinOtpMapper;
    @Mock private OneTimePasswordMapper oneTimePasswordMapper;
    @Mock private OTPServiceFactory otpServiceFactory;
    @Mock private MemberQueryBuilder memberQueryBuilder;
    @Mock private MemberAssembler memberAssembler;
    @Mock private ApplicationReviewHandler applicationReviewHandler;
    @Mock private MemberStatusHandler memberStatusHandler;
    @Mock private MemberSubmissionHandler memberSubmissionHandler;
    @Mock private LockService lockService;

    private OrganizationMemberServiceImpl service;
    private RecordingTransactionManager transactions;
    private DistributedLockService.Lock lock;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OrganizationMember.class);
        transactions = new RecordingTransactionManager();
        lock = mock(DistributedLockService.Lock.class);
        lenient().when(lockService.tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS)).thenReturn(lock);
        lenient().when(organizationMemberMapper.selectOne(any())).thenReturn(operator(InviteConstants.ROLE_ORG_OWNER, Status.ENABLED));
        service = new OrganizationMemberServiceImpl(clock, groupApplicationMapper, organizationMemberMapper,
                organizationJoinOtpMapper, oneTimePasswordMapper, otpServiceFactory, memberQueryBuilder, memberAssembler,
                applicationReviewHandler, memberStatusHandler, memberSubmissionHandler, lockService, transactions);
    }

    @Test
    void shouldLockBeforeTransactionAndClearSessionOnlyAfterCommit() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED));
        transactions.onBegin = () -> verify(lockService).tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS);
        transactions.onCommit = () -> verify(lock, never()).close();

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_ADMIN));

        InOrder order = inOrder(lockService, organizationMemberMapper, memberStatusHandler, lock);
        order.verify(lockService).tryAcquireOrThrow("org:member:role:1", 0, TimeUnit.SECONDS);
        order.verify(organizationMemberMapper).selectOne(any());
        order.verify(memberStatusHandler).getAndValidateMember(2L, 1L);
        order.verify(lock).close();
        assertThat(transactions.propagation).isEqualTo(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(organizationMemberMapper).update(eq(null), any());
    }

    @Test
    void shouldDemoteEnabledAdminWhileOwnerRemains() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));

        service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER));

        verify(organizationMemberMapper).update(eq(null), any());
        verify(organizationMemberMapper, never()).selectCount(any());
        verify(lock).close();
        assertThat(transactions.rollbacks).isEqualTo(0);
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
    void shouldRejectAdminOperatorBeforeReadingTarget() {
        when(organizationMemberMapper.selectOne(any())).thenReturn(operator(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());

        verify(memberStatusHandler, never()).getAndValidateMember(anyLong(), anyLong());
        verify(organizationMemberMapper, never()).update(eq(null), any());
    }

    @Test
    void shouldRejectAssigningOrChangingOwner() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_ADMIN, Status.ENABLED));

        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_OWNER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不能分配组织所有者");
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_OWNER, Status.ENABLED));
        assertThatThrownBy(() -> service.changeMemberRole(roleParams(InviteConstants.ROLE_ORG_USER)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不能修改组织所有者的角色");

        verify(organizationMemberMapper, never()).update(eq(null), any());
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
    void shouldDisableMemberUnderLock() {
        when(memberStatusHandler.getAndValidateMember(2L, 1L)).thenReturn(member(InviteConstants.ROLE_ORG_USER, Status.ENABLED));

        service.changeMemberStatus(statusParams(Status.DISABLED));

        verify(memberStatusHandler).updateMemberStatus(any(), eq(Status.DISABLED));
        verify(lock).close();
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

    @Test
    void listJoinInviteCodesReturnsTheDatabasePage() {
        OrgAdminJoinInviteCodeResultBO visible = OrgAdminJoinInviteCodeResultBO.builder()
                .id(13L).inviteCode("CCCCCC").status("PENDING").build();
        Page<OrgAdminJoinInviteCodeResultBO> page = new Page<>(2, 5);
        page.setRecords(List.of(visible));
        page.setTotal(6);
        when(organizationJoinOtpMapper.selectActiveJoinInviteCodePage(
                any(), eq(1L), eq(OTPStatus.PENDING), eq(OTPStatus.USED), eq(OTPType.OrganizationUserInvite),
                eq(LocalDateTime.now(clock)))).thenReturn(page);

        PageResultBO<OrgAdminJoinInviteCodeResultBO> result = service.listJoinInviteCodes(
                OrgAdminJoinInviteCodeListParamsBO.builder()
                        .orgId(1L).status("PENDING").page(new PageParamsBO(2, 5)).build());

        assertThat(result.getTotal()).isEqualTo(6);
        assertThat(result.getList()).containsExactly(visible);
    }

    @Test
    void listJoinInviteCodesRejectsUnknownStatus() {
        assertThatThrownBy(() -> service.listJoinInviteCodes(OrgAdminJoinInviteCodeListParamsBO.builder()
                .orgId(1L).status("NOPE").page(new PageParamsBO(1, 10)).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码状态不正确");
        verify(organizationJoinOtpMapper, never()).selectActiveJoinInviteCodePage(any(), any(), any(), any(), any(), any());
    }

    @Test
    void revokeJoinInviteCodeRejectsAnotherOrganization() {
        when(organizationJoinOtpMapper.selectById(11L)).thenReturn(
                OrganizationJoinOtp.builder().id(11L).orgId(2L).otpId(101L).build());

        assertThatThrownBy(() -> service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder()
                .orgId(1L).inviteId(11L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码不存在");
        verify(oneTimePasswordMapper, never()).update(isNull(), any());
    }

    @Test
    void revokeJoinInviteCodeRejectsExhaustedCode() {
        when(organizationJoinOtpMapper.selectById(12L)).thenReturn(
                OrganizationJoinOtp.builder().id(12L).orgId(1L).otpId(102L).build());
        when(oneTimePasswordMapper.selectById(102L)).thenReturn(
                invite(102L, "BBBBBB", OTPStatus.PENDING, 1, 1, LocalDateTime.parse("2026-10-27T09:00:00")));

        assertThatThrownBy(() -> service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder()
                .orgId(1L).inviteId(12L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不能失效");
    }

    @Test
    void revokeJoinInviteCodeMarksPendingCodeRevoked() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OneTimePassword.class);
        when(organizationJoinOtpMapper.selectById(13L)).thenReturn(
                OrganizationJoinOtp.builder().id(13L).orgId(1L).otpId(103L).build());
        when(oneTimePasswordMapper.selectById(103L)).thenReturn(
                invite(103L, "CCCCCC", OTPStatus.PENDING, 0, null, LocalDateTime.parse("2026-10-27T11:00:00")));
        when(oneTimePasswordMapper.update(isNull(), any())).thenReturn(1);

        service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder().orgId(1L).inviteId(13L).build());

        verify(oneTimePasswordMapper).update(isNull(), any());
    }

    @Test
    void revokeJoinInviteCodeRejectsMissingOrExpiredPassword() {
        when(organizationJoinOtpMapper.selectById(13L)).thenReturn(
                OrganizationJoinOtp.builder().id(13L).orgId(1L).otpId(103L).build());
        when(oneTimePasswordMapper.selectById(103L)).thenReturn(null);

        assertThatThrownBy(() -> service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder()
                .orgId(1L).inviteId(13L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("邀请码不存在");

        OneTimePassword expired = invite(103L, "CCCCCC", OTPStatus.PENDING, 0, null, LocalDateTime.parse("2026-10-27T11:00:00"));
        expired.setExpiredAt(LocalDateTime.parse("2026-09-21T00:00:00"));
        when(oneTimePasswordMapper.selectById(103L)).thenReturn(expired);
        assertThatThrownBy(() -> service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder()
                .orgId(1L).inviteId(13L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已过期");
        verify(oneTimePasswordMapper, never()).update(isNull(), any());
    }

    @Test
    void revokeJoinInviteCodeTreatsLostUpdateAsExpired() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), OneTimePassword.class);
        when(organizationJoinOtpMapper.selectById(13L)).thenReturn(
                OrganizationJoinOtp.builder().id(13L).orgId(1L).otpId(103L).build());
        when(oneTimePasswordMapper.selectById(103L)).thenReturn(
                invite(103L, "CCCCCC", OTPStatus.PENDING, 0, null, LocalDateTime.parse("2026-10-27T11:00:00")));
        when(oneTimePasswordMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> service.revokeJoinInviteCode(OrgAdminRevokeJoinInviteCodeParamsBO.builder()
                .orgId(1L).inviteId(13L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已过期");
    }

    private OneTimePassword invite(Long id, String code, OTPStatus status, int useCount, Integer maxUses, LocalDateTime createdAt) {
        return OneTimePassword.builder().id(id).code(code).bizType(OTPType.OrganizationUserInvite)
                .status(status).useCount(useCount).maxUses(maxUses).createdAt(createdAt).build();
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

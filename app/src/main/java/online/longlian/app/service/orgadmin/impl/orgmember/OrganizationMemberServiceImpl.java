package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberRemoveParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberTransferOwnershipParamsBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class OrganizationMemberServiceImpl implements OrganizationMemberService {

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);

    private final Clock clock;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final UserMapper userMapper;
    private final OTPServiceFactory otpServiceFactory;
    private final MemberQueryBuilder memberQueryBuilder;
    private final MemberAssembler memberAssembler;
    private final ApplicationReviewHandler applicationReviewHandler;
    private final MemberStatusHandler memberStatusHandler;
    private final MemberSubmissionHandler memberSubmissionHandler;
    private final LockService lockService;
    private final SessionService sessionService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public PageResultBO<OrgAdminApplicationInfoResultBO> listApplications(@NonNull OrgAdminApplicationListParamsBO params) {
        LambdaQueryWrapper<GroupApplication> queryWrapper = memberQueryBuilder.buildApplicationListQuery(params);
        Page<GroupApplication> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<GroupApplication> applicationPage = groupApplicationMapper.selectPage(page, queryWrapper);
        if (applicationPage.getRecords().isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), applicationPage.getTotal());
        }
        return new PageResultBO<>(memberAssembler.assembleApplications(applicationPage.getRecords()), applicationPage.getTotal());
    }

    @Override
    public void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params) {
        String lockKey = "org:application:review:" + params.getApplicationId();
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(lockKey, 0, 5, TimeUnit.SECONDS)) {
            requireManager(params.getOrgId(), params.getReviewerId());
            GroupApplication application = groupApplicationMapper.selectById(params.getApplicationId());
            applicationReviewHandler.review(application, params.getOrgId(), params.getApplicationStatus(),
                    params.getReviewerId(), params.getReviewRemark(), LocalDateTime.now(clock));
        }
    }

    @Override
    public PageResultBO<OrgMemberInfoResultBO> listMembers(@NonNull OrgMemberListParamsBO params) {
        LambdaQueryWrapper<OrganizationMember> queryWrapper = memberQueryBuilder.buildMemberListQuery(params);
        Page<OrganizationMember> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<OrganizationMember> memberPage = organizationMemberMapper.selectPage(page, queryWrapper);
        if (memberPage.getRecords().isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), memberPage.getTotal());
        }
        return new PageResultBO<>(memberAssembler.assembleMembers(memberPage.getRecords()), memberPage.getTotal());
    }

    @Override
    public void changeMemberStatus(@NonNull OrgMemberChangeStatusParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            assertCanManageTarget(params.getOrgId(), params.getOperatorUserId(), member);
            memberStatusHandler.updateMemberStatus(member, params.getStatus());
            revokeUserSessionsAfterCommit(member.getUserId());
        });
    }

    @Override
    public void changeMemberRole(@NonNull OrgMemberChangeRoleParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            assertOwnerCanManageTarget(params.getOrgId(), params.getOperatorUserId(), member);
            updateMemberRole(member.getId(), params.getOrgRole());
            revokeUserSessionsAfterCommit(member.getUserId());
        });
    }

    @Override
    public void removeMember(@NonNull OrgMemberRemoveParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            assertCanManageTarget(params.getOrgId(), params.getOperatorUserId(), member);
            deleteMemberAndClearDefaultOrganization(member);
            revokeUserSessionsAfterCommit(member.getUserId());
        });
    }

    @Override
    public void exitOrganization(@NonNull OrgMemberRemoveParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            OrganizationMember member = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                    .eq(OrganizationMember::getOrgId, params.getOrgId())
                    .eq(OrganizationMember::getUserId, params.getOperatorUserId())
                    .last("LIMIT 1"));
            if (member == null) {
                throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
            }
            if (InviteConstants.ROLE_ORG_OWNER.equals(member.getOrgRole())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "组织所有者转让所有权后才能退出组织");
            }
            deleteMemberAndClearDefaultOrganization(member);
            revokeUserSessionsAfterCommit(member.getUserId());
        });
    }

    @Override
    public void transferOwnership(@NonNull OrgMemberTransferOwnershipParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            OrganizationMember target = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            OrganizationMember owner = requireOwner(params.getOrgId(), params.getOperatorUserId());
            if (target.getStatus() != Status.ENABLED) {
                throw new AppException(ResultCode.OPERATION_FAIL, "只能将所有权转让给启用状态成员");
            }
            if (target.getUserId().equals(owner.getUserId()) || InviteConstants.ROLE_ORG_OWNER.equals(target.getOrgRole())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "所有权转让目标无效");
            }
            int updated = organizationMapper.update(null, new LambdaUpdateWrapper<Organization>()
                    .eq(Organization::getId, params.getOrgId())
                    .eq(Organization::getOwnerUserId, owner.getUserId())
                    .set(Organization::getOwnerUserId, target.getUserId())
                    .set(Organization::getUpdatedAt, LocalDateTime.now(clock)));
            if (updated != 1) {
                throw new AppException(ResultCode.OPERATION_FAIL, "组织所有权已变更，请刷新后重试");
            }
            updateMemberRole(owner.getId(), InviteConstants.ROLE_ORG_ADMIN);
            updateMemberRole(target.getId(), InviteConstants.ROLE_ORG_OWNER);
            revokeUserSessionsAfterCommit(owner.getUserId());
            revokeUserSessionsAfterCommit(target.getUserId());
        });
    }

    @Override
    public OrgMemberBaseTaskSubmitCountResultBO getMemberBaseTaskSubmitCounts(OrgMemberBaseTaskSubmitCountParamsBO params) {
        OrganizationMember member = organizationMemberMapper.selectById(params.getMemberId());
        if (member == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        }
        if (!params.getOrgId().equals(member.getOrgId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权操作该成员");
        }
        return memberSubmissionHandler.getSubmitCounts(member);
    }

    @Override
    public OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params) {
        requireManager(params.getOrgId(), params.getCreatorId());
        OneTimePassword oneTimePassword = otpServiceFactory.get(OTPType.OrganizationUserInvite).generate(
                OTPGenerateContextBO.builder().creatorId(params.getCreatorId()).orgId(params.getOrgId()).build());
        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(oneTimePassword.getCode())
                .expireAt(oneTimePassword.getExpiredAt().format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    private void changeMemberUnderLock(Long orgId, Runnable mutation) {
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(
                "org:member:role:" + orgId, 0, TimeUnit.SECONDS)) {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.executeWithoutResult(status -> mutation.run());
        }
    }

    private void assertCanManageTarget(Long orgId, Long operatorUserId, OrganizationMember target) {
        OrganizationMember operator = requireManager(orgId, operatorUserId);
        if (target.getUserId().equals(operatorUserId) || InviteConstants.ROLE_ORG_OWNER.equals(target.getOrgRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能操作组织所有者或自己");
        }
        if (InviteConstants.ROLE_ORG_ADMIN.equals(operator.getOrgRole())
                && !InviteConstants.ROLE_ORG_USER.equals(target.getOrgRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "普通管理员只能管理普通成员");
        }
    }

    private void assertOwnerCanManageTarget(Long orgId, Long operatorUserId, OrganizationMember target) {
        requireOwner(orgId, operatorUserId);
        if (target.getUserId().equals(operatorUserId) || InviteConstants.ROLE_ORG_OWNER.equals(target.getOrgRole())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能直接变更组织所有者角色");
        }
    }

    private OrganizationMember requireManager(Long orgId, Long userId) {
        OrganizationMember operator = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getUserId, userId)
                .eq(OrganizationMember::getStatus, Status.ENABLED)
                .last("LIMIT 1"));
        if (operator == null || (!InviteConstants.ROLE_ORG_OWNER.equals(operator.getOrgRole())
                && !InviteConstants.ROLE_ORG_ADMIN.equals(operator.getOrgRole()))) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "当前用户无权管理组织成员");
        }
        return operator;
    }

    private OrganizationMember requireOwner(Long orgId, Long userId) {
        OrganizationMember owner = requireManager(orgId, userId);
        Organization organization = organizationMapper.selectById(orgId);
        if (!InviteConstants.ROLE_ORG_OWNER.equals(owner.getOrgRole())
                || organization == null || !userId.equals(organization.getOwnerUserId())) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "只有组织所有者可以执行此操作");
        }
        return owner;
    }

    private void deleteMemberAndClearDefaultOrganization(OrganizationMember member) {
        organizationMemberMapper.deleteById(member.getId());
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .eq(User::getId, member.getUserId())
                .eq(User::getDefaultOrgId, member.getOrgId())
                .set(User::getDefaultOrgId, 0L));
    }

    private void updateMemberRole(Long memberId, String orgRole) {
        organizationMemberMapper.update(null, new LambdaUpdateWrapper<OrganizationMember>()
                .eq(OrganizationMember::getId, memberId)
                .set(OrganizationMember::getOrgRole, orgRole)
                .set(OrganizationMember::getUpdatedAt, LocalDateTime.now(clock)));
    }

    private void revokeUserSessionsAfterCommit(Long userId) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                sessionService.clearUserSessionCache(userId);
            }
        });
    }
}

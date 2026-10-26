package online.longlian.app.service.orgadmin.impl.orgmember;

import org.springframework.dao.DuplicateKeyException;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberChangeRoleParamsBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.common.LockService;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
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

    private static final DateTimeFormatter DEFAULT_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern(InviteConstants.DEFAULT_DATE_TIME_PATTERN);


    private final Clock clock;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OTPServiceFactory otpServiceFactory;

    private final MemberQueryBuilder memberQueryBuilder;
    private final MemberAssembler memberAssembler;
    private final ApplicationReviewHandler applicationReviewHandler;
    private final MemberStatusHandler memberStatusHandler;
    private final MemberSubmissionHandler memberSubmissionHandler;
    private final LockService lockService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public PageResultBO<OrgAdminApplicationInfoResultBO> listApplications(@NonNull OrgAdminApplicationListParamsBO params) {
        LambdaQueryWrapper<GroupApplication> queryWrapper = memberQueryBuilder.buildApplicationListQuery(params);
        Page<GroupApplication> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<GroupApplication> applicationPage = groupApplicationMapper.selectPage(page, queryWrapper);
        List<GroupApplication> applications = applicationPage.getRecords();
        if (applications.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), applicationPage.getTotal());
        }
        return new PageResultBO<>(memberAssembler.assembleApplications(applications), applicationPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params) {
        try {
            GroupApplication application = groupApplicationMapper.selectOne(
                    new LambdaQueryWrapper<GroupApplication>()
                            .eq(GroupApplication::getId, params.getApplicationId())
                            .last("FOR UPDATE"));
            applicationReviewHandler.review(application, params.getOrgId(), params.getApplicationStatus(),
                    params.getReviewerId(), params.getReviewRemark(), LocalDateTime.now(clock));
        } catch (DuplicateKeyException e) {
            throw new AppException(ResultCode.OPERATION_FAIL, "用户名、邮箱或成员关系已被占用");
        }
    }

    @Override
    public PageResultBO<OrgMemberInfoResultBO> listMembers(@NonNull OrgMemberListParamsBO params) {
        LambdaQueryWrapper<OrganizationMember> queryWrapper = memberQueryBuilder.buildMemberListQuery(params);
        Page<OrganizationMember> page = new Page<>(params.getPage().getPageNum(), params.getPage().getPageSize());
        Page<OrganizationMember> memberPage = organizationMemberMapper.selectPage(page, queryWrapper);
        List<OrganizationMember> members = memberPage.getRecords();
        if (members.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), memberPage.getTotal());
        }
        return new PageResultBO<>(memberAssembler.assembleMembers(members), memberPage.getTotal());
    }

    @Override
    public void changeMemberStatus(@NonNull OrgMemberChangeStatusParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            requireEnabledOperator(params.getOrgId(), params.getOperatorUserId(),
                    InviteConstants.ROLE_ORG_ADMIN, InviteConstants.ROLE_ORG_OWNER);
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            memberStatusHandler.validateNotAdminDisable(member, params.getStatus());
            memberStatusHandler.updateMemberStatus(member, params.getStatus());
        });
    }

    /**
     * 调整成员在当前组织内的角色。只有所有者可以操作，且不能改动或分配所有者。
     */
    @Override
    public void changeMemberRole(@NonNull OrgMemberChangeRoleParamsBO params) {
        changeMemberUnderLock(params.getOrgId(), () -> {
            requireEnabledOperator(params.getOrgId(), params.getOperatorUserId(), InviteConstants.ROLE_ORG_OWNER);
            if (InviteConstants.ROLE_ORG_OWNER.equals(params.getOrgRole().name())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "不能分配组织所有者");
            }
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            if (InviteConstants.ROLE_ORG_OWNER.equals(member.getOrgRole())) {
                throw new AppException(ResultCode.OPERATION_FAIL, "不能修改组织所有者的角色");
            }

            updateMemberRole(member.getId(), params.getOrgRole().name());
        });
    }

    /**
     * 锁必须先于事务内的首次读取，并保持到事务提交；否则并发请求可能沿用锁前建立的旧快照。
     */
    private void changeMemberUnderLock(Long orgId, Runnable mutation) {
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(
                "org:member:role:" + orgId, 0, TimeUnit.SECONDS)) {
            TransactionTemplate transaction = new TransactionTemplate(transactionManager);
            transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            transaction.executeWithoutResult(status -> mutation.run());
        }
    }

    /**
     * 入口鉴权可能早于角色被改掉，修改前必须按数据库中的当前身份重新授权。
     */
    private void requireEnabledOperator(Long orgId, Long operatorUserId, String... allowedRoles) {
        OrganizationMember operator = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getUserId, operatorUserId));
        if (operator == null || operator.getStatus() != Status.ENABLED || !hasRole(operator.getOrgRole(), allowedRoles)) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "当前用户无权调整组织成员");
        }
    }

    private boolean hasRole(String role, String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (allowedRole.equals(role)) {
                return true;
            }
        }
        return false;
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
        OneTimePassword otp = otpServiceFactory.get(OTPType.OrganizationUserInvite).generate(
                OTPGenerateContextBO.builder()
                        .creatorId(params.getCreatorId())
                        .orgId(params.getOrgId())
                        .expiredAt(params.getExpiredAt())
                        .build()
        );
        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(otp.getCode())
                .expireAt(otp.getExpiredAt().format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }

    private void updateMemberRole(Long memberId, String orgRole) {
        organizationMemberMapper.update(null,
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getId, memberId)
                        .set(OrganizationMember::getOrgRole, orgRole)
                        .set(OrganizationMember::getUpdatedAt, LocalDateTime.now(clock)));
    }

}

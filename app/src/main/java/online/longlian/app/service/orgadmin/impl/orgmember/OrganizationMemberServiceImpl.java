package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.common.OTPGenerateContextBO;
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
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.LockService;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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
    private final SessionService sessionService;

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

    /**
     * 先校验申请有效性，再根据审批结果执行通过/拒绝操作，
     */
    @Override
    public void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params) {
        String lockKey = "org:application:review:" + params.getApplicationId();
        try (DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(lockKey, 0, 5, TimeUnit.SECONDS)) {
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
        List<OrganizationMember> members = memberPage.getRecords();
        if (members.isEmpty()) {
            return new PageResultBO<>(Collections.emptyList(), memberPage.getTotal());
        }
        return new PageResultBO<>(memberAssembler.assembleMembers(members), memberPage.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeMemberStatus(@NonNull OrgMemberChangeStatusParamsBO params) {
        OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
        memberStatusHandler.validateNotAdminDisable(member, params.getStatus());
        memberStatusHandler.updateMemberStatus(member, params.getStatus());
        if (params.getStatus() == Status.DISABLED) {
            sessionService.clearUserSessionCache(member.getUserId());
        }
    }

    /**
     * 调整成员在当前组织内的角色。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeMemberRole(@NonNull OrgMemberChangeRoleParamsBO params) {
        OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
        if (isDemotingAdmin(member, params)) {
            demoteAdminWithLock(params);
        } else {
            updateMemberRole(member.getId(), params.getOrgRole());
        }
        clearRoleSessionCacheAfterCommit(member.getUserId());
    }

    /**
     * 只有「管理员 → 普通用户」会减少管理员数量，也只有这条路径需要额外的最后一名管理员校验。
     */
    private boolean isDemotingAdmin(OrganizationMember member, OrgMemberChangeRoleParamsBO params) {
        return InviteConstants.ROLE_ORG_ADMIN.equals(member.getOrgRole())
                && InviteConstants.ROLE_ORG_USER.equals(params.getOrgRole());
    }

    /**
     * 持锁执行降级，锁必须覆盖「复核 → 更新 → 事务提交」全过程：
     * 若锁在事务提交前就释放，两个并发降级会各自在未提交的事务里看到多名管理员，
     * 双双通过校验并提交，最终组织一名启用管理员都不剩。
     */
    private void demoteAdminWithLock(OrgMemberChangeRoleParamsBO params) {
        DistributedLockService.Lock lock = lockService.tryAcquireOrThrow(
                "org:member:role:" + params.getOrgId(), 0, TimeUnit.SECONDS);
        boolean releaseAfterCompletion = deferLockReleaseUntilCompletion(lock);
        try {
            // 等锁期间角色可能已被并发请求改掉，拿锁后必须重读一次再判定
            OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
            validateNotLastEnabledAdmin(member, params.getOrgId());
            updateMemberRole(member.getId(), params.getOrgRole());
        } finally {
            // 事务存在时由 afterCompletion 负责放锁，这里再关会提前释放
            if (!releaseAfterCompletion) {
                lock.close();
            }
        }
    }

    /**
     * 把锁的释放推迟到事务结束后，返回是否注册成功。
     * 无事务上下文（如单元测试直接调用）时返回 false，交由调用方立即释放。
     */
    private boolean deferLockReleaseUntilCompletion(DistributedLockService.Lock lock) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return false;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                lock.close();
            }
        });
        return true;
    }

    /**
     * 组织必须至少保留一名启用状态的管理员，否则成员会失去全部管理入口。
     */
    private void validateNotLastEnabledAdmin(OrganizationMember member, Long orgId) {
        // 复核后已不是管理员（并发场景下被抢先降级），无需再校验数量
        if (!InviteConstants.ROLE_ORG_ADMIN.equals(member.getOrgRole())) {
            return;
        }
        long enabledAdminCount = organizationMemberMapper.selectCount(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getOrgRole, InviteConstants.ROLE_ORG_ADMIN)
                .eq(OrganizationMember::getStatus, Status.ENABLED));
        if (enabledAdminCount <= 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织至少保留一名管理员");
        }
    }

    /**
     * 角色缓存在登录会话里，必须等事务提交后再清：提交前清缓存的话，
     * 并发请求可能用旧角色把缓存重新填满，而提交后不会再被清第二次。
     */
    private void clearRoleSessionCacheAfterCommit(Long userId) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sessionService.clearUserSessionCache(userId);
                }
            });
            return;
        }
        sessionService.clearUserSessionCache(userId);
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
        OneTimePassword oneTimePassword = otpServiceFactory.get(OTPType.OrganizationUserInvite).generate(
                OTPGenerateContextBO.builder()
                        .creatorId(params.getCreatorId())
                        .orgId(params.getOrgId())
                        .build()
        );
        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(oneTimePassword.getCode())
                .expireAt(oneTimePassword.getExpiredAt().format(DEFAULT_DATE_TIME_FORMATTER))
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

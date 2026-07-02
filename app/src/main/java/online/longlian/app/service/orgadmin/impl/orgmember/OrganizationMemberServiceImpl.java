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
import online.longlian.app.pojo.bo.orgadmin.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.OTPType;
import online.longlian.app.service.common.LockService;
import online.longlian.common.service.DistributedLockService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserMapper userMapper;
    private final Clock clock;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final ResourceService resourceService;
    private final OTPServiceFactory otpServiceFactory;
    private final OneTimePasswordService oneTimePasswordService;

    private final MemberQueryBuilder memberQueryBuilder;
    private final MemberAssembler memberAssembler;
    private final ApplicationReviewHandler applicationReviewHandler;
    private final MemberStatusHandler memberStatusHandler;
    private final MemberSubmissionHandler memberSubmissionHandler;
    private final LockService lockService;

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
}

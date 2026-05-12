package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.*;
import online.longlian.app.pojo.bo.OTPGenerateContextBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountParamsBO;
import online.longlian.app.pojo.bo.OrgMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.entity.*;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.app.service.otp.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.ApplicationType;
import online.longlian.common.enumeration.OTPType;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;


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
    @Transactional(rollbackFor = Exception.class)
    public void reviewApplication(@NonNull OrgAdminReviewApplicationParamsBO params) {
        GroupApplication application = groupApplicationMapper.selectById(params.getApplicationId());
        applicationReviewHandler.validatePendingApplication(application, params.getOrgId());

        LocalDateTime now = LocalDateTime.now(clock);
        Long approvedUserId = null;
        if (params.getApplicationStatus() == ApplicationStatus.APPROVED) {
            OrganizationMember newMember = applicationReviewHandler.approveApplication(application);
            approvedUserId = newMember.getUserId();
            backfillOrganizationJoinOtp(application, approvedUserId, newMember.getId());
        } else if (params.getApplicationStatus() == ApplicationStatus.REJECTED) {
            applicationReviewHandler.rejectApplication(application);
        }

        applicationReviewHandler.updateApplicationStatus(
                application, params.getApplicationStatus(), params.getReviewerId(),
                params.getReviewRemark(), approvedUserId, now);
    }

    private void backfillOrganizationJoinOtp(GroupApplication application, Long userId, Long orgMemberId) {
        LambdaQueryWrapper<OrganizationJoinOtp> queryWrapper = new LambdaQueryWrapper<OrganizationJoinOtp>()
                .eq(OrganizationJoinOtp::getOrgId, application.getOrgId())
                .orderByDesc(OrganizationJoinOtp::getId);

        if (application.getApplicationType() == ApplicationType.EXISTING_USER) {
            queryWrapper.eq(OrganizationJoinOtp::getInvitedUserId, application.getUserId());
        } else {
            queryWrapper.isNull(OrganizationJoinOtp::getInvitedUserId);
        }

        Page<OrganizationJoinOtp> page = new Page<>(1, 1);
        OrganizationJoinOtp joinOtp = organizationJoinOtpMapper.selectPage(page, queryWrapper)
                .getRecords().stream().findFirst().orElse(null);
        if (joinOtp == null) {
            return;
        }

        LambdaUpdateWrapper<OrganizationJoinOtp> updateWrapper = new LambdaUpdateWrapper<OrganizationJoinOtp>()
                .eq(OrganizationJoinOtp::getId, joinOtp.getId())
                .set(OrganizationJoinOtp::getOrgMemberId, orgMemberId);
        if (application.getApplicationType() == ApplicationType.REGISTER) {
            updateWrapper.set(OrganizationJoinOtp::getInvitedUserId, userId);
        }
        organizationJoinOtpMapper.update(null, updateWrapper);
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

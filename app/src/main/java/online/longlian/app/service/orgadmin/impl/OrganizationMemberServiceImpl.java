package online.longlian.app.service.orgadmin.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.pojo.bo.OneTimePasswordCreateParamsBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminApplicationListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.OrgAdminGenerateJoinOrgInviteCodeResultBO;
import online.longlian.app.pojo.bo.OrgnMemberBaseTaskSubmitCountResultBO;
import online.longlian.app.pojo.bo.OrgMemberChangeStatusParamsBO;
import online.longlian.app.pojo.bo.OrgMemberInfoResultBO;
import online.longlian.app.pojo.bo.OrgMemberListParamsBO;
import online.longlian.app.pojo.bo.OrgAdminReviewApplicationParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.common.CodeGenerator;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.generator.enumeration.ApplicationStatus;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
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

    private final Clock clock;
    private final CodeGenerator codeGenerator;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMapper organizationMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
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
            approvedUserId = applicationReviewHandler.approveApplication(application);
        } else if (params.getApplicationStatus() == ApplicationStatus.REJECTED) {
            applicationReviewHandler.rejectApplication(application);
        }

        applicationReviewHandler.updateApplicationStatus(
                application, params.getApplicationStatus(), params.getReviewerId(),
                params.getReviewRemark(), approvedUserId, now);
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
    public OrgnMemberBaseTaskSubmitCountResultBO getMemberBaseTaskSubmitCounts(Long memberId) {
        OrganizationMember member = organizationMemberMapper.selectById(memberId);
        if (member == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "成员不存在");
        }
        return memberSubmissionHandler.getSubmitCounts(member);
    }

    @Override
    public OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(@NonNull OrgAdminGenerateJoinOrgInviteCodeParamsBO params) {
        Organization organization = organizationMapper.selectById(params.getOrgId());
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
        LocalDateTime expiredAt = LocalDateTime.now(clock).plusMinutes(InviteConstants.INVITE_EXPIRE_MINUTES);
        String inviteCode = codeGenerator.generate(InviteConstants.INVITE_CODE_LENGTH);

        OneTimePassword oneTimePassword = oneTimePasswordService.generateOTP(
                OneTimePasswordCreateParamsBO.builder()
                        .code(inviteCode)
                        .expiredAt(expiredAt)
                        .bizType(OTPType.OrganizationUserInvite)
                        .creatorId(params.getCreatorId())
                        .build()
        );

        OrganizationJoinOtp organizationJoinOtp = OrganizationJoinOtp.builder()
                .otpId(oneTimePassword.getId())
                .orgId(organization.getId())
                .build();
        organizationJoinOtpMapper.insert(organizationJoinOtp);

        return OrgAdminGenerateJoinOrgInviteCodeResultBO.builder()
                .inviteCode(inviteCode)
                .expireAt(expiredAt.format(DEFAULT_DATE_TIME_FORMATTER))
                .build();
    }
}

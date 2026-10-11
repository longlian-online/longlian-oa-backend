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
import online.longlian.app.service.common.OrganizationAuthorizationService;
import online.longlian.app.service.orgadmin.OrganizationMemberService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.common.enumeration.OTPType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;

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
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OTPServiceFactory otpServiceFactory;

    private final MemberQueryBuilder memberQueryBuilder;
    private final MemberAssembler memberAssembler;
    private final ApplicationReviewHandler applicationReviewHandler;
    private final MemberStatusHandler memberStatusHandler;
    private final MemberSubmissionHandler memberSubmissionHandler;
    private final OrganizationAuthorizationService authorization;
    private final OrganizationMemberPolicy policy;

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
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void reviewApplication(OrgAdminReviewApplicationParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        authorization.requireManager(params.getOrgId(), params.getReviewerId());
        GroupApplication application = groupApplicationMapper.selectOne(new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getId, params.getApplicationId()).last("FOR UPDATE"));
        applicationReviewHandler.review(application, params.getOrgId(), params.getApplicationStatus(),
                params.getReviewerId(), params.getReviewRemark(), LocalDateTime.now(clock));
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
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void changeMemberStatus(OrgMemberChangeStatusParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        OrganizationMember operator = authorization.requireManager(params.getOrgId(), params.getOperatorUserId());
        OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
        policy.requireManageableTarget(operator, member);
        memberStatusHandler.updateMemberStatus(member, params.getStatus());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public void changeMemberRole(OrgMemberChangeRoleParamsBO params) {
        authorization.lockOrganization(params.getOrgId(), true);
        OrganizationMember operator = authorization.requireManager(params.getOrgId(), params.getOperatorUserId());
        OrganizationMember member = memberStatusHandler.getAndValidateMember(params.getMemberId(), params.getOrgId());
        policy.requireRoleChange(operator, member);
        if (!member.getOrgRole().equals(params.getOrgRole().name()))
            updateMemberRole(member.getId(), params.getOrgRole().name());
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
    @Transactional(rollbackFor = Exception.class, isolation = Isolation.READ_COMMITTED)
    public OrgAdminGenerateJoinOrgInviteCodeResultBO generateJoinOrgInviteCode(OrgAdminGenerateJoinOrgInviteCodeParamsBO params) {
        authorization.requireManager(params.getOrgId(), params.getCreatorId());
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

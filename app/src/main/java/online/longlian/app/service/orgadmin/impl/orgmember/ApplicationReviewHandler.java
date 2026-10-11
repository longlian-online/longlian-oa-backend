package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.ApplicationStatus;
import online.longlian.common.enumeration.ApplicationType;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class ApplicationReviewHandler {
    private final UserMapper userMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;

    /** 调用方已锁组织和申请；启用账号、建立成员关系与审核结果必须一起提交。 */
    @Transactional(propagation = Propagation.MANDATORY)
    public void review(GroupApplication application, Long orgId, ApplicationStatus outcome,
                       Long reviewerId, String reviewRemark, LocalDateTime now) {
        if (application == null || !orgId.equals(application.getOrgId()))
            throw new AppException(ResultCode.DATA_NOT_EXIT, "入组申请不存在");
        if (application.getStatus() != ApplicationStatus.PENDING)
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        if (outcome == ApplicationStatus.APPROVED) {
            User user = applicationUser(application);
            OrganizationMember existing = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                    .eq(OrganizationMember::getOrgId, orgId).eq(OrganizationMember::getUserId, user.getId())
                    .last("LIMIT 1 FOR UPDATE"));
            if (existing != null)
                throw new AppException(ResultCode.OPERATION_FAIL, "申请人已有组织成员关系");
            // 本 PR 保留现有注册模型：提交时建立禁用用户，审批通过后启用；快照改造另行处理。
            if (application.getApplicationType() == ApplicationType.REGISTER) {
                user.setStatus(Status.ENABLED);
                user.setDefaultOrgId(orgId);
                user.setUpdatedAt(now);
                userMapper.updateById(user);
            }
            OrganizationMember member = OrganizationMember.builder()
                    .orgId(orgId).userId(user.getId()).orgRole(InviteConstants.ROLE_ORG_USER)
                    .joinedAt(now).submitCount(0).status(Status.ENABLED).createdAt(now).updatedAt(now).build();
            organizationMemberMapper.insert(member);
            backfillInvite(application, member);
        }
        groupApplicationMapper.update(null, new LambdaUpdateWrapper<GroupApplication>()
                .eq(GroupApplication::getId, application.getId())
                .set(GroupApplication::getStatus, outcome).set(GroupApplication::getReviewerId, reviewerId)
                .set(GroupApplication::getReviewedAt, now).set(GroupApplication::getReviewRemark, reviewRemark == null ? "" : reviewRemark)
                .set(GroupApplication::getUpdatedAt, now));
    }

    private User applicationUser(GroupApplication application) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getId, application.getUserId()).last("FOR UPDATE"));
        if (user == null) throw new AppException(ResultCode.USER_NOT_EXIT);
        switch (application.getApplicationType()) {
            case REGISTER -> {
                if (user.getStatus() != Status.DISABLED)
                    throw new AppException(ResultCode.OPERATION_FAIL, "注册申请对应用户状态无效");
            }
            case EXISTING_USER -> {
                if (user.getStatus() != Status.ENABLED)
                    throw new AppException(ResultCode.OPERATION_FAIL, "申请人已被禁用");
            }
            case null -> throw new AppException(ResultCode.OPERATION_FAIL, "申请类型无效");
        }
        return user;
    }

    private void backfillInvite(GroupApplication application, OrganizationMember member) {
        OrganizationJoinOtp invite = organizationJoinOtpMapper.selectOne(new LambdaQueryWrapper<OrganizationJoinOtp>()
                .eq(OrganizationJoinOtp::getOtpId, application.getOtpId())
                .orderByDesc(OrganizationJoinOtp::getId).last("LIMIT 1"));
        if (invite == null) return;
        LambdaUpdateWrapper<OrganizationJoinOtp> update = new LambdaUpdateWrapper<OrganizationJoinOtp>()
                .eq(OrganizationJoinOtp::getId, invite.getId()).set(OrganizationJoinOtp::getOrgMemberId, member.getId());
        if (application.getApplicationType() == ApplicationType.REGISTER)
            update.set(OrganizationJoinOtp::getInvitedUserId, member.getUserId());
        organizationJoinOtpMapper.update(null, update);
    }
}

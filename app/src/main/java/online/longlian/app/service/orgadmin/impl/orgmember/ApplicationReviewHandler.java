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

import java.time.LocalDateTime;

/**
 * 处理入组申请的审批流程：校验、通过、拒绝、状态更新。
 */
@Component
@RequiredArgsConstructor
public class ApplicationReviewHandler {

    private final UserMapper userMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;

    private void validatePendingApplication(GroupApplication application, Long orgId) {
        if (application == null || !orgId.equals(application.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "入组申请不存在");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }
    }

    /**
     * 通过申请：注册快照转为正式用户，已有用户仅建立成员关系
     */
    private OrganizationMember approveApplication(GroupApplication application, LocalDateTime now) {
        if (application.getApplicationType() == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请类型无效");
        }
        User user = switch (application.getApplicationType()) {
            case REGISTER -> createRegisteredUser(application, now);
            case EXISTING_USER -> getExistingApplicationUser(application);
        };

        OrganizationMember organizationMember = OrganizationMember.builder()
                .orgId(application.getOrgId())
                .userId(user.getId())
                .orgRole(InviteConstants.ROLE_ORG_USER)
                .joinedAt(now)
                .submitCount(0)
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationMemberMapper.insert(organizationMember);

        return organizationMember;
    }

    private User createRegisteredUser(GroupApplication application, LocalDateTime now) {
        if (application.getUserId() != null || application.getPasswordHash() == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "注册申请需要先完成快照转换");
        }
        User user = User.builder().username(application.getUsername()).email(application.getEmail())
                .nickname(application.getNickname()).password(application.getPasswordHash())
                .status(Status.ENABLED).defaultOrgId(application.getOrgId()).createdAt(now).updatedAt(now).build();
        userMapper.insert(user);
        return user;
    }

    private User getExistingApplicationUser(GroupApplication application) {
        if (application.getUserId() == null || application.getPasswordHash() != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "已有用户申请快照无效");
        }
        User user = userMapper.selectById(application.getUserId());
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        if (user.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请人已被禁用");
        }
        return user;
    }

    /**
     * 更新申请状态：仅当申请仍为 PENDING 时才生效。
     */
    private void updateApplicationStatus(GroupApplication application, ApplicationStatus status,
                                         Long reviewerId, String reviewRemark,
                                         Long approvedUserId, LocalDateTime now) {
        LambdaUpdateWrapper<GroupApplication> updateWrapper = new LambdaUpdateWrapper<GroupApplication>()
                .eq(GroupApplication::getId, application.getId())
                .eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .set(GroupApplication::getStatus, status)
                .set(GroupApplication::getPasswordHash, null)
                .set(GroupApplication::getReviewerId, reviewerId)
                .set(GroupApplication::getReviewedAt, now)
                .set(GroupApplication::getReviewRemark, reviewRemark == null ? "" : reviewRemark)
                .set(GroupApplication::getUpdatedAt, now);
        if (approvedUserId != null) {
            updateWrapper.set(GroupApplication::getUserId, approvedUserId);
        }
        if (groupApplicationMapper.update(null, updateWrapper) != 1) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }
    }

    public void review(GroupApplication application, Long orgId, ApplicationStatus applicationStatus,
                       Long reviewerId, String reviewRemark, LocalDateTime now) {
        validatePendingApplication(application, orgId);

        Long approvedUserId = null;
        if (applicationStatus == ApplicationStatus.APPROVED) {
            OrganizationMember newMember = approveApplication(application, now);
            approvedUserId = newMember.getUserId();
            backfillOrganizationJoinOtp(application, approvedUserId, newMember.getId());
        }

        updateApplicationStatus(application, applicationStatus, reviewerId,
                reviewRemark, approvedUserId, now);
    }

    private void backfillOrganizationJoinOtp(GroupApplication application, Long userId, Long orgMemberId) {
        LambdaQueryWrapper<OrganizationJoinOtp> queryWrapper = new LambdaQueryWrapper<OrganizationJoinOtp>()
                .eq(OrganizationJoinOtp::getOtpId, application.getOtpId())
                .orderByDesc(OrganizationJoinOtp::getId)
                .last("LIMIT 1");

        OrganizationJoinOtp joinOtp = organizationJoinOtpMapper.selectOne(queryWrapper);
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
}

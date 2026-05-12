package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.generator.enumeration.ApplicationStatus;
import online.longlian.generator.enumeration.Status;
import org.springframework.stereotype.Component;

import java.time.Clock;
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
    private final Clock clock;

    public void validatePendingApplication(GroupApplication application, Long orgId) {
        if (application == null || !orgId.equals(application.getOrgId())) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "入组申请不存在");
        }
        if (application.getStatus() != ApplicationStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "该申请已审核");
        }
    }

    /**
     * 通过申请：REGISTER 类型需同步创建用户账户
     */
    public OrganizationMember approveApplication(GroupApplication application) {
        LocalDateTime now = LocalDateTime.now(clock);
        User user = switch (application.getApplicationType()) {
            case REGISTER -> createUserByApplication(application, now);
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

    public void rejectApplication(GroupApplication application) {
        if (application == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "入组申请不存在");
        }
    }

    public User createUserByApplication(GroupApplication application, LocalDateTime now) {
        User user = User.builder()
                .username(application.getUsername())
                .password(application.getPassword())
                .nickname(application.getNickname())
                .email(application.getEmail())
                .status(Status.ENABLED)
                .defaultOrgId(application.getOrgId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        userMapper.insert(user);
        return user;
    }

    public User getExistingApplicationUser(GroupApplication application) {
        User user = userMapper.selectById(application.getUserId());
        // 校验链：用户必须存在、未被禁用、且未重复加入同一组织
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        if (user.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请人已被禁用");
        }
        OrganizationMember existedMember = organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, application.getOrgId())
                .eq(OrganizationMember::getUserId, user.getId())
                .last("LIMIT 1"));
        if (existedMember != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "申请人已加入该组织");
        }
        return user;
    }

    /**
     * 更新申请状态：仅当申请仍为 PENDING 时才生效。
     */
    public void updateApplicationStatus(GroupApplication application, ApplicationStatus status,
                                         Long reviewerId, String reviewRemark,
                                         Long approvedUserId, LocalDateTime now) {
        LambdaUpdateWrapper<GroupApplication> updateWrapper = new LambdaUpdateWrapper<GroupApplication>()
                .eq(GroupApplication::getId, application.getId())
                .eq(GroupApplication::getStatus, ApplicationStatus.PENDING)
                .set(GroupApplication::getStatus, status)
                .set(GroupApplication::getReviewerId, reviewerId)
                .set(GroupApplication::getReviewedAt, now)
                .set(GroupApplication::getReviewRemark, reviewRemark == null ? "" : reviewRemark)
                .set(GroupApplication::getUpdatedAt, now);
        if (approvedUserId != null) {
            updateWrapper.set(GroupApplication::getUserId, approvedUserId);
        }
        groupApplicationMapper.update(null, updateWrapper);
    }
}

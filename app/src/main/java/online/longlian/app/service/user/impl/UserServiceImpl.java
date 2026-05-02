package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationCreateOtpMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationCreateOtp;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.user.SessionService;
import online.longlian.app.service.user.UserService;
import online.longlian.generator.enumeration.ApplicationStatus;
import online.longlian.generator.enumeration.ApplicationType;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final VerifyCodeService verifyCodeService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OrganizationCreateOtpMapper organizationCreateOtpMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final ResourceService resourceService;
    private final UserMapper userMapper;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;
    private final OneTimePasswordService oneTimePasswordService;

    @Override
    public UserGetMyInfoResultBO getMyInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        UserGetMyInfoResultBO.UserGetMyInfoResultBOBuilder builder = UserGetMyInfoResultBO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .defaultOrgId(user.getDefaultOrgId());
        if (user.getAvatarFileId() != null) {
            builder.avatarUrl(resourceService.getResourceReadUrl(user.getAvatarFileId()));
        }
        return builder.build();
    }

    @Override
    public UserSwitchOrgResultBO switchOrg(UserSwitchOrgParamsBO params) {
        currentOrganizationService.switchCurrentOrg(params.getUserId(), params.getOrgId());

        Organization organization = organizationMapper.selectById(params.getOrgId());
        OrganizationMember organizationMember = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, params.getUserId())
                        .eq(OrganizationMember::getOrgId, params.getOrgId())
                        .last("LIMIT 1")
        );

        String avatarUrl = null;
        if (organization.getAvatarFileId() != null) {
            avatarUrl = resourceService.getResourceReadUrl(organization.getAvatarFileId());
        }

        return UserSwitchOrgResultBO.builder()
                .id(organization.getId())
                .name(organization.getName())
                .avatarUrl(avatarUrl)
                .roles(organizationMember == null || organizationMember.getOrgRole() == null
                        ? List.of()
                        : List.of(organizationMember.getOrgRole()))
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAndCreateOrganizationByInvite(UserRegisterByInviteParamsBO params) {
        validateRegisterRequest(params);
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationInvite);
        OrganizationCreateOtp organizationCreateOtp = validatePendingOrganizationCreateInvitation(oneTimePassword.getId());
        if (params.getOrgName() == null || params.getOrgName().isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "组织名称不能为空");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = createUser(params, now);

        Organization organization = Organization.builder()
                .name(params.getOrgName().trim())
                .status(Status.ENABLED)
                .creatorId(user.getId())
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationMapper.insert(organization);

        OrganizationMember organizationMember = OrganizationMember.builder()
                .orgId(organization.getId())
                .userId(user.getId())
                .orgRole(InviteConstants.ROLE_ORG_ADMIN)
                .joinedAt(now)
                .submitCount(0)
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationMemberMapper.insert(organizationMember);

        user.setDefaultOrgId(organization.getId());
        userMapper.updateById(user);

        oneTimePasswordService.useOTP(oneTimePassword.getId());
        organizationCreateOtpMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationCreateOtp>()
                        .eq(OrganizationCreateOtp::getId, organizationCreateOtp.getId())
                        .isNull(OrganizationCreateOtp::getInvitedUserId)
                        .set(OrganizationCreateOtp::getInvitedUserId, user.getId())
                        .set(OrganizationCreateOtp::getOrgId, organization.getId())
                        .set(OrganizationCreateOtp::getUsedAt, now)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params) {
        validateRegisterRequest(params);
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationUserInvite);
        OrganizationJoinOtp organizationJoinOtp = validatePendingOrganizationJoinInvitation(oneTimePassword.getId());

        Organization organization = validateJoinTargetOrganization(organizationJoinOtp.getOrgId());

        LocalDateTime now = LocalDateTime.now();
        GroupApplication groupApplication = GroupApplication.builder()
                .orgId(organization.getId())
                .userId(0L)
                .status(ApplicationStatus.PENDING)
                .applicationType(ApplicationType.REGISTER)
                .username(params.getUsername())
                .password(passwordEncoder.encode(params.getPassword()))
                .nickname(params.getNickname())
                .email(params.getEmail())
                .createdAt(now)
                .updatedAt(now)
                .build();
        groupApplicationMapper.insert(groupApplication);

        oneTimePasswordService.useOTP(oneTimePassword.getId());
        organizationJoinOtpMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationJoinOtp>()
                        .eq(OrganizationJoinOtp::getId, organizationJoinOtp.getId())
                        .isNull(OrganizationJoinOtp::getInvitedUserId)
                        .set(OrganizationJoinOtp::getUsedAt, now)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinOrganizationByInvite(Long userId, String inviteCode) {
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(inviteCode, OTPType.OrganizationUserInvite);
        OrganizationJoinOtp organizationJoinOtp = validatePendingOrganizationJoinInvitation(oneTimePassword.getId());
        Organization organization = validateJoinTargetOrganization(organizationJoinOtp.getOrgId());

        OrganizationMember existedMember = organizationMemberMapper.selectOne(
                new LambdaQueryWrapper<OrganizationMember>()
                        .eq(OrganizationMember::getUserId, userId)
                        .eq(OrganizationMember::getOrgId, organization.getId())
                        .last("LIMIT 1")
        );
        if (existedMember != null) {
            if (existedMember.getStatus() == Status.ENABLED) {
                throw new AppException(ResultCode.OPERATION_FAIL, "您已加入该组织");
            }
            throw new AppException(ResultCode.OPERATION_FAIL, "您在该组织中的成员状态已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupApplication groupApplication = GroupApplication.builder()
                .orgId(organization.getId())
                .userId(userId)
                .status(ApplicationStatus.PENDING)
                .applicationType(ApplicationType.EXISTING_USER)
                .createdAt(now)
                .updatedAt(now)
                .build();
        groupApplicationMapper.insert(groupApplication);

        oneTimePasswordService.useOTP(oneTimePassword.getId());
        organizationJoinOtpMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationJoinOtp>()
                        .eq(OrganizationJoinOtp::getId, organizationJoinOtp.getId())
                        .isNull(OrganizationJoinOtp::getInvitedUserId)
                        .set(OrganizationJoinOtp::getInvitedUserId, userId)
                        .set(OrganizationJoinOtp::getUsedAt, now)
        );
    }

    @Override
    public UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params) {
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationUserInvite);
        OrganizationJoinOtp organizationJoinOtp = validatePendingOrganizationJoinInvitation(oneTimePassword.getId());
        Organization organization = validateJoinTargetOrganization(organizationJoinOtp.getOrgId());
        return UserGetJoinOrgInviteInfoResultBO.builder()
                .orgId(organization.getId())
                .orgName(organization.getName())
                .build();
    }

    private void validateRegisterRequest(UserRegisterByInviteParamsBO params) {
        if (verifyCodeService.validateCode(params.getEmail(), params.getCode())) {
            throw new AppException(ResultCode.PARAM_ERROR, "邮箱验证码错误");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, params.getUsername())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, params.getEmail())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邮箱已存在");
        }
    }

    private Organization validateJoinTargetOrganization(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不存在");
        }
        if (organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织已被禁用");
        }
        return organization;
    }

    private User createUser(UserRegisterByInviteParamsBO params, LocalDateTime now) {
        User user = User.builder()
                .username(params.getUsername())
                .password(passwordEncoder.encode(params.getPassword()))
                .nickname(params.getNickname())
                .email(params.getEmail())
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .defaultOrgId(0L)
                .build();
        userMapper.insert(user);
        return user;
    }

    private OrganizationCreateOtp validatePendingOrganizationCreateInvitation(Long otpId) {
        OrganizationCreateOtp organizationCreateOtp = organizationCreateOtpMapper.selectOne(
                new LambdaQueryWrapper<OrganizationCreateOtp>()
                        .eq(OrganizationCreateOtp::getOtpId, otpId)
                        .last("LIMIT 1")
        );
        if (organizationCreateOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        validateInvitationAvailable(organizationCreateOtp.getInvitedUserId(), organizationCreateOtp.getUsedAt());
        return organizationCreateOtp;
    }

    private OrganizationJoinOtp validatePendingOrganizationJoinInvitation(Long otpId) {
        OrganizationJoinOtp organizationJoinOtp = organizationJoinOtpMapper.selectOne(
                new LambdaQueryWrapper<OrganizationJoinOtp>()
                        .eq(OrganizationJoinOtp::getOtpId, otpId)
                        .last("LIMIT 1")
        );
        if (organizationJoinOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        validateInvitationAvailable(organizationJoinOtp.getInvitedUserId(), organizationJoinOtp.getUsedAt());
        return organizationJoinOtp;
    }

    private void validateInvitationAvailable(Long invitedUserId, LocalDateTime usedAt) {
        if (invitedUserId != null || usedAt != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已使用");
        }
    }
}

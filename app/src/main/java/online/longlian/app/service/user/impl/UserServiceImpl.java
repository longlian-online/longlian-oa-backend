package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationJoinOtpMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.OTPUseContextBO;
import online.longlian.app.pojo.bo.OTPValidateContextBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.UserGetMyInfoResultBO;
import online.longlian.app.pojo.bo.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationJoinOtp;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.otp.OTPServiceFactory;
import online.longlian.app.service.resource.ResourceService;
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

    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OrganizationJoinOtpMapper organizationJoinOtpMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final ResourceService resourceService;
    private final UserMapper userMapper;
    private final CurrentOrganizationService currentOrganizationService;
    private final OTPServiceFactory otpServiceFactory;

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
        OneTimePassword emailOtp = validateRegisterRequest(params);
        OneTimePassword inviteOtp = otpServiceFactory.get(OTPType.OrganizationInvite).getValid(
                OTPValidateContextBO.builder().code(params.getInviteCode()).build());
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

        otpServiceFactory.get(OTPType.EmailVerify).use(
                OTPUseContextBO.builder().otpId(emailOtp.getId()).build());
        otpServiceFactory.get(OTPType.OrganizationInvite).use(
                OTPUseContextBO.builder()
                        .otpId(inviteOtp.getId())
                        .userId(user.getId())
                        .orgId(organization.getId())
                        .build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params) {
        OneTimePassword emailOtp = validateRegisterRequest(params);
        OneTimePassword inviteOtp = otpServiceFactory.get(OTPType.OrganizationUserInvite).getValid(
                OTPValidateContextBO.builder().code(params.getInviteCode()).build());
        Organization organization = getJoinTargetOrganization(inviteOtp);

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

        otpServiceFactory.get(OTPType.EmailVerify).use(
                OTPUseContextBO.builder().otpId(emailOtp.getId()).build());
        otpServiceFactory.get(OTPType.OrganizationUserInvite).use(
                OTPUseContextBO.builder().otpId(inviteOtp.getId()).build());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void joinOrganizationByInvite(Long userId, String inviteCode) {
        OneTimePassword inviteOtp = otpServiceFactory.get(OTPType.OrganizationUserInvite).getValid(
                OTPValidateContextBO.builder().code(inviteCode).build());
        Organization organization = getJoinTargetOrganization(inviteOtp);

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

        otpServiceFactory.get(OTPType.OrganizationUserInvite).use(
                OTPUseContextBO.builder()
                        .otpId(inviteOtp.getId())
                        .userId(userId)
                        .build());
    }

    @Override
    public UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params) {
        OneTimePassword inviteOtp = otpServiceFactory.get(OTPType.OrganizationUserInvite).getValid(
                OTPValidateContextBO.builder().code(params.getInviteCode()).build());
        Organization organization = getJoinTargetOrganization(inviteOtp);
        return UserGetJoinOrgInviteInfoResultBO.builder()
                .orgId(organization.getId())
                .orgName(organization.getName())
                .build();
    }

    private OneTimePassword validateRegisterRequest(UserRegisterByInviteParamsBO params) {
        OneTimePassword emailOtp = otpServiceFactory.get(OTPType.EmailVerify).getValid(
                OTPValidateContextBO.builder().code(params.getCode()).target(params.getEmail()).build());
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, params.getUsername())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, params.getEmail())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邮箱已存在");
        }
        return emailOtp;
    }

    private Organization getJoinTargetOrganization(OneTimePassword inviteOtp) {
        OrganizationJoinOtp joinOtp = organizationJoinOtpMapper.selectOne(
                new LambdaQueryWrapper<OrganizationJoinOtp>()
                        .eq(OrganizationJoinOtp::getOtpId, inviteOtp.getId())
                        .last("LIMIT 1")
        );
        if (joinOtp == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        Organization organization = organizationMapper.selectById(joinOtp.getOrgId());
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
}

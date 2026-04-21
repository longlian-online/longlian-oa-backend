package online.longlian.app.service.user.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.InviteConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.OrganizationCreateOptMapper;
import online.longlian.app.mapper.OrganizationJoinOptMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoParamsBO;
import online.longlian.app.pojo.bo.UserGetJoinOrgInviteInfoResultBO;
import online.longlian.app.pojo.bo.UserRegisterByInviteParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgParamsBO;
import online.longlian.app.pojo.bo.UserSwitchOrgResultBO;
import online.longlian.app.pojo.entity.OneTimePassword;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationCreateOpt;
import online.longlian.app.pojo.entity.OrganizationJoinOpt;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.common.OneTimePasswordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.user.SessionService;
import online.longlian.app.service.user.UserService;
import online.longlian.generator.enumeration.OPTStatus;
import online.longlian.generator.enumeration.OTPType;
import online.longlian.generator.enumeration.Status;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final VerifyCodeService verifyCodeService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final OrganizationCreateOptMapper organizationCreateOptMapper;
    private final OrganizationJoinOptMapper organizationJoinOptMapper;
    private final ResourceService resourceService;
    private final UserMapper userMapper;
    private final SessionService sessionService;
    private final CurrentOrganizationService currentOrganizationService;
    private final OneTimePasswordService oneTimePasswordService;

    @Override
    public Result<UserInfoVO> getMyInfo() {
        Long userId = sessionService.getCurrentUserId();
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResultCode.USER_NOT_EXIT);
        }
        UserInfoVO.UserInfoVOBuilder builder = UserInfoVO.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .defaultOrgId(user.getDefaultOrgId());
        if (user.getAvatarFileId() != null) {
            builder.avatarUrl(resourceService.getFileAccessUrl(user.getAvatarFileId()));
        }
        return Result.success("查询成功", builder.build());
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
            avatarUrl = resourceService.getFileAccessUrl(organization.getAvatarFileId());
        }

        return UserSwitchOrgResultBO.builder()
                .id(organization.getId())
                .name(organization.getName())
                .avatarUrl(avatarUrl)
                .role(organizationMember == null ? null : organizationMember.getOrgRole())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAndCreateOrganizationByInvite(UserRegisterByInviteParamsBO params) {
        validateRegisterRequest(params);
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationInvite);
        OrganizationCreateOpt organizationCreateOpt = validatePendingOrganizationCreateInvitation(oneTimePassword.getId());
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
        organizationCreateOptMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationCreateOpt>()
                        .eq(OrganizationCreateOpt::getId, organizationCreateOpt.getId())
                        .eq(OrganizationCreateOpt::getStatus, OPTStatus.PENDING)
                        .set(OrganizationCreateOpt::getInvitedUserId, user.getId())
                        .set(OrganizationCreateOpt::getOrgId, organization.getId())
                        .set(OrganizationCreateOpt::getUsedAt, now)
                        .set(OrganizationCreateOpt::getStatus, OPTStatus.USED)
        );
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerAndJoinOrganizationByInvite(UserRegisterByInviteParamsBO params) {
        validateRegisterRequest(params);
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationUserInvite);
        OrganizationJoinOpt organizationJoinOpt = validatePendingOrganizationJoinInvitation(oneTimePassword.getId());

        Organization organization = organizationMapper.selectById(organizationJoinOpt.getOrgId());
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不存在或已被禁用");
        }

        LocalDateTime now = LocalDateTime.now();
        User user = createUser(params, now);

        OrganizationMember organizationMember = OrganizationMember.builder()
                .orgId(organization.getId())
                .userId(user.getId())
                .orgRole(InviteConstants.ROLE_ORG_USER)
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
        organizationJoinOptMapper.update(
                null,
                new LambdaUpdateWrapper<OrganizationJoinOpt>()
                        .eq(OrganizationJoinOpt::getId, organizationJoinOpt.getId())
                        .eq(OrganizationJoinOpt::getStatus, OPTStatus.PENDING)
                        .set(OrganizationJoinOpt::getInvitedUserId, user.getId())
                        .set(OrganizationJoinOpt::getOrgMemberId, organizationMember.getId())
                        .set(OrganizationJoinOpt::getUsedAt, now)
                        .set(OrganizationJoinOpt::getStatus, OPTStatus.USED)
        );
    }

    @Override
    public UserGetJoinOrgInviteInfoResultBO getJoinOrgInviteInfo(UserGetJoinOrgInviteInfoParamsBO params) {
        OneTimePassword oneTimePassword = oneTimePasswordService.getValidOTP(params.getInviteCode(), OTPType.OrganizationUserInvite);
        OrganizationJoinOpt organizationJoinOpt = validatePendingOrganizationJoinInvitation(oneTimePassword.getId());

        Organization organization = organizationMapper.selectById(organizationJoinOpt.getOrgId());
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不存在或已被禁用");
        }
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

    private OrganizationCreateOpt validatePendingOrganizationCreateInvitation(Long otpId) {
        OrganizationCreateOpt organizationCreateOpt = organizationCreateOptMapper.selectOne(
                new LambdaQueryWrapper<OrganizationCreateOpt>()
                        .eq(OrganizationCreateOpt::getOtpId, otpId)
                        .last("LIMIT 1")
        );
        if (organizationCreateOpt == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        validateInvitationStatus(organizationCreateOpt.getStatus(), organizationCreateOpt.getInvitedUserId());
        return organizationCreateOpt;
    }

    private OrganizationJoinOpt validatePendingOrganizationJoinInvitation(Long otpId) {
        OrganizationJoinOpt organizationJoinOpt = organizationJoinOptMapper.selectOne(
                new LambdaQueryWrapper<OrganizationJoinOpt>()
                        .eq(OrganizationJoinOpt::getOtpId, otpId)
                        .last("LIMIT 1")
        );
        if (organizationJoinOpt == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在");
        }
        validateInvitationStatus(organizationJoinOpt.getStatus(), organizationJoinOpt.getInvitedUserId());
        return organizationJoinOpt;
    }

    private void validateInvitationStatus(OPTStatus status, Long invitedUserId) {
        if (status == OPTStatus.USED || invitedUserId != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已使用");
        }
        if (status == OPTStatus.EXPIRED) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码已过期");
        }
        if (status != OPTStatus.PENDING) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码状态异常");
        }
    }
}

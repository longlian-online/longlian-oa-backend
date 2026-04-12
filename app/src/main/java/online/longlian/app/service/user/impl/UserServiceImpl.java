package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.enumeration.InviteMode;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.enumeration.ApplicationStatus;
import online.longlian.app.common.enumeration.Status;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.mapper.GroupApplicationMapper;
import online.longlian.app.mapper.OrganizationMapper;
import online.longlian.app.mapper.OrganizationMemberMapper;
import online.longlian.app.mapper.RoleMapper;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.mapper.UserRoleMapper;
import online.longlian.app.pojo.bo.InviteCodeCacheBO;
import online.longlian.app.pojo.dto.app.JoinByInviteCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByCodeDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.Role;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.entity.UserRole;
import online.longlian.app.pojo.vo.app.LoginVO;
import online.longlian.app.pojo.vo.app.UserInfoVO;
import online.longlian.app.service.VerifyCodeService;
import online.longlian.app.service.resource.FileStorageService;
import online.longlian.app.service.user.UserService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final RedisBlacklistUtil redisBlacklistUtil;
    private final VerifyCodeService verifyCodeService;
    private final PasswordEncoder passwordEncoder;
    private final OrganizationMapper organizationMapper;
    private final OrganizationMemberMapper organizationMemberMapper;
    private final GroupApplicationMapper groupApplicationMapper;
    private final RoleMapper roleMapper;
    private final UserRoleMapper userRoleMapper;
    private final FileStorageService fileStorageService;
    private final UserMapper userMapper;
    @Override
    public Result<LoginVO> loginByPwd(LoginByPwdDTO loginByPwdDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new MyUsernamePasswordAuthenticationToken(
                                loginByPwdDTO.getUsername(),
                                loginByPwdDTO.getPassword()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    public Result<LoginVO> loginByCode(LoginByCodeDTO loginByCodeDTO) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new EmailCodeAuthenticationToken(
                                loginByCodeDTO.getEmail(),
                                loginByCodeDTO.getCode()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> registerByInvite(RegisterByInviteDTO registerByInviteDTO) {
        // 先校验邀请码、邮箱验证码和账号唯一性
        validateRegisterRequest(registerByInviteDTO);

        // 从 Redis 中读取邀请码上下文
        InviteCodeCacheBO inviteData = getInviteCodeData(registerByInviteDTO.getInviteCode());
        InviteMode inviteMode = inviteData.getInviteMode();
        Long orgId = inviteData.getOrgId();

        LocalDateTime now = LocalDateTime.now();
        User user = User.builder()
                .username(registerByInviteDTO.getUsername())
                .password(passwordEncoder.encode(registerByInviteDTO.getPassword()))
                .nickname(registerByInviteDTO.getNickname())
                .email(registerByInviteDTO.getEmail())
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .defaultOrgId(0L)
                .build();
        userMapper.insert(user);

        // 根据邀请码类型执行“创建组织”“注册后直接入组”或拒绝处理。
        if (InviteMode.SUPER_ADMIN_CREATE_ORG.equals(inviteMode)) {
            createOrgForInvitedUser(registerByInviteDTO, user, now);
        } else if (InviteMode.ORG_ADMIN_REGISTER_JOIN.equals(inviteMode)) {
            joinOrgAfterRegister(orgId, user, now);
        } else {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前邀请码不支持注册");
        }

        // 邀请码为一次性使用，注册成功后删除
        redisTemplate.delete(RedisConstants.INVITE_CODE + registerByInviteDTO.getInviteCode());
        return Result.success("注册成功");
    }

    @Override
    public Result<UserInfoVO> getMyInfo() {
        Long userId = ThreadLocalUtil.getUserBO().getId();
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
            builder.avatarUrl(fileStorageService.getFileAccessUrl(user.getAvatarFileId()));
        }
        return Result.success("查询成功", builder.build());
    }

    private Result<LoginVO> doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();

        String token = jwtUtil.generateToken(userId);

        // 登录成功后缓存当前用户和默认组织
        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER + userId,
                JSON.toJSONString(userDetail),
                RedisConstants.EXPIRE_TIME,
                TimeUnit.SECONDS
        );
        redisTemplate.opsForValue().set(
                RedisConstants.CURRENT_ORG + userId,
                userDetail.getDefaultOrgId(),
                RedisConstants.EXPIRE_TIME,
                TimeUnit.SECONDS
        );
        LoginVO loginVO = LoginVO.builder()
                .userId(userId)
                .token(token)
                .roles(userDetail.getRoles())
                .defaultOrgId(userDetail.getDefaultOrgId())
                .build();
        return Result.success("登录成功", loginVO);
    }

    @Override
    public Result<Void> logout(HttpServletRequest request) {
        String token = (String) request.getAttribute(CommonConstants.CURRENT_TOKEN);
        if (token == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        long remainingSeconds = jwtUtil.getRemainingTimeSeconds(token);
        redisBlacklistUtil.addToBlacklist(token, remainingSeconds);

        Long userId = ThreadLocalUtil.getUserBO().getId();
        redisTemplate.delete(RedisConstants.LOGIN_USER + userId);

        return Result.success("登出成功");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> joinByInviteCode(JoinByInviteCodeDTO joinByInviteCodeDTO) {
        Long userId = ThreadLocalUtil.getUserBO().getId();
        InviteCodeCacheBO inviteData = getInviteCodeData(joinByInviteCodeDTO.getInviteCode());
        // 仅“管理员邀请已注册用户入组”场景允许已登录用户提交入组申请。
        if (inviteData.getInviteMode() != InviteMode.ORG_ADMIN_MEMBER_JOIN) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前邀请码不支持加入组织");
        }
        Long orgId = inviteData.getOrgId();
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null || organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }

        // 已是成员或已有申请时不允许重复提交
        if (organizationMemberMapper.selectOne(new LambdaQueryWrapper<OrganizationMember>()
                .eq(OrganizationMember::getOrgId, orgId)
                .eq(OrganizationMember::getUserId, userId)) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "你已加入该组织");
        }
        if (groupApplicationMapper.selectOne(new LambdaQueryWrapper<GroupApplication>()
                .eq(GroupApplication::getOrgId, orgId)
                .eq(GroupApplication::getUserId, userId)) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "请勿重复提交入组申请");
        }

        LocalDateTime now = LocalDateTime.now();
        GroupApplication application = GroupApplication.builder()
                .orgId(orgId)
                .userId(userId)
                .status(ApplicationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        groupApplicationMapper.insert(application);
        // 邀请码一次性使用，提交申请后删除
        redisTemplate.delete(RedisConstants.INVITE_CODE + joinByInviteCodeDTO.getInviteCode());
        return Result.success("申请已提交，等待管理员审核");
    }

    private void validateRegisterRequest(RegisterByInviteDTO registerByInviteDTO) {
        if (!registerByInviteDTO.getPassword().equals(registerByInviteDTO.getConfirmPassword())) {
            throw new AppException(ResultCode.PARAM_ERROR, "两次输入的密码不一致");
        }
        if (!verifyCodeService.validateCode(registerByInviteDTO.getEmail(), registerByInviteDTO.getCode())) {
            throw new AppException(ResultCode.PARAM_ERROR, "邮箱验证码错误");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername, registerByInviteDTO.getUsername())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "用户名已存在");
        }
        if (userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, registerByInviteDTO.getEmail())) != null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邮箱已存在");
        }
    }

    private void createOrgForInvitedUser(RegisterByInviteDTO registerByInviteDTO, User user, LocalDateTime now) {
        if (registerByInviteDTO.getOrgName() == null || registerByInviteDTO.getOrgName().isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, "组织名称不能为空");
        }

        // 超管邀请场景：注册后直接创建组织并初始化管理员身份
        Organization organization = Organization.builder()
                .name(registerByInviteDTO.getOrgName().trim())
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
        bindRole(user.getId(), now);
    }

    private void joinOrgAfterRegister(Long orgId, User user, LocalDateTime now) {
        if (orgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码缺少组织信息");
        }
        ensureOrgExists(orgId);

        // 管理员邀请注册场景：注册成功后直接加入组织，并将默认组织设置为邀请组织。
        OrganizationMember organizationMember = OrganizationMember.builder()
                .orgId(orgId)
                .userId(user.getId())
                .orgRole(InviteConstants.ROLE_ORG_USER)
                .joinedAt(now)
                .submitCount(0)
                .status(Status.ENABLED)
                .createdAt(now)
                .updatedAt(now)
                .build();
        organizationMemberMapper.insert(organizationMember);

        user.setDefaultOrgId(orgId);
        userMapper.updateById(user);
    }

    private void createJoinApplication(Long orgId, Long userId, LocalDateTime now) {
        if (orgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码缺少组织信息");
        }
        ensureOrgExists(orgId);

        // 管理员邀请已注册用户场景：提交入组申请，等待管理员审核。
        GroupApplication application = GroupApplication.builder()
                .orgId(orgId)
                .userId(userId)
                .status(ApplicationStatus.PENDING)
                .createdAt(now)
                .updatedAt(now)
                .build();
        groupApplicationMapper.insert(application);
    }

    private void ensureOrgExists(Long orgId) {
        Organization organization = organizationMapper.selectById(orgId);
        if (organization == null|| organization.getStatus() == Status.DISABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "组织不存在或已禁用");
        }
    }

    private InviteCodeCacheBO getInviteCodeData(String inviteCode) {
        InviteCodeCacheBO inviteCodeData = (InviteCodeCacheBO) redisTemplate.opsForValue().get(RedisConstants.INVITE_CODE + inviteCode);
        if (inviteCodeData == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请码不存在或已过期");
        }
        return inviteCodeData;
    }

    private void bindRole(Long userId, LocalDateTime now) {
        Role role = roleMapper.selectOne(new LambdaQueryWrapper<Role>().eq(Role::getRoleCode, InviteConstants.ROLE_ORG_ADMIN));
        if (role == null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "角色不存在: " + InviteConstants.ROLE_ORG_ADMIN);
        }
        // 绑定系统角色，确保后续权限判断生效
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .roleId(role.getId())
                .createAt(now)
                .updateAt(now)
                .build();
        userRoleMapper.insert(userRole);
    }
}

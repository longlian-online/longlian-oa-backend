package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.InviteConstants;
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
import online.longlian.app.pojo.bo.InviteCacheBO;
import online.longlian.app.pojo.dto.LoginByCodeDTO;
import online.longlian.app.pojo.dto.LoginByPwdDTO;
import online.longlian.app.pojo.dto.RegisterByInviteDTO;
import online.longlian.app.pojo.entity.GroupApplication;
import online.longlian.app.pojo.entity.Organization;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.pojo.entity.Role;
import online.longlian.app.pojo.entity.User;
import online.longlian.app.pojo.entity.UserRole;
import online.longlian.app.pojo.vo.LoginVO;
import online.longlian.app.pojo.vo.UserInfoVO;
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

        // 从 Redis 中读取邀请链接上下文
        InviteCacheBO inviteData = getInviteData(registerByInviteDTO.getInviteToken());
        String inviteMode = inviteData.getInviteMode();
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

        // 根据邀请模式执行“建组织”或“申请入组”
        if (InviteConstants.INVITE_MODE_SUPER_ADMIN_CREATE_ORG.equals(inviteMode)) {
            createOrgForInvitedUser(registerByInviteDTO, user, now);
        } else if (InviteConstants.INVITE_MODE_ORG_ADMIN_JOIN.equals(inviteMode)) {
            createJoinApplication(orgId, user.getId(), now);
        } else {
            throw new AppException(ResultCode.OPERATION_FAIL, "未知邀请类型");
        }

        // 邀请链接为一次性使用，注册成功后删除
        redisTemplate.delete(RedisConstants.INVITE_LINK + registerByInviteDTO.getInviteToken());
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

    private InviteCacheBO getInviteData(String inviteToken) {
        InviteCacheBO inviteData = (InviteCacheBO) redisTemplate.opsForValue().get(RedisConstants.INVITE_LINK + inviteToken);
        if (inviteData == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请链接不存在或已过期");
        }
        return inviteData;
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

    private void createJoinApplication(Long orgId, Long userId, LocalDateTime now) {
        if (orgId == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "邀请链接缺少组织信息");
        }
        ensureOrgExists(orgId);

        // 管理员邀请场景：注册后先生成待审核入组申请
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

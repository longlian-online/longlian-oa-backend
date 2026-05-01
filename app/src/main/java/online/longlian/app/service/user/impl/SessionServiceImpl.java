package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.LoginSessionCacheBO;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.pojo.bo.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.SessionLoginResultBO;
import online.longlian.app.pojo.bo.SessionLogoutParamsBO;
import online.longlian.app.service.common.CurrentOrganizationService;
import online.longlian.app.service.user.SessionService;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final CurrentOrganizationService currentOrganizationService;

    @Override
    public SessionLoginResultBO loginByPwd(SessionLoginByPwdParamsBO params) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new MyUsernamePasswordAuthenticationToken(
                                params.getUsername(),
                                params.getPassword()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    public SessionLoginResultBO loginByCode(SessionLoginByCodeParamsBO params) {
        Authentication authentication =
                authenticationManager.authenticate(
                        new EmailCodeAuthenticationToken(
                                params.getEmail(),
                                params.getCode()
                        )
                );
        return doLogin(authentication);
    }

    @Override
    public void logout(SessionLogoutParamsBO params) {
        if (params.getToken() == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        long remainingSeconds = jwtUtil.getRemainingTimeSeconds(params.getToken());
        tokenBlacklistService.addToBlacklist(params.getToken(), null, params.getUserId(), "用户登出", remainingSeconds);

        redisTemplate.delete(RedisConstants.LOGIN_USER + params.getUserId());
        currentOrganizationService.clearCurrentOrg(params.getUserId());
    }

    @Override
    public void refreshCurrentUserOrg(Long currentOrgId, List<String> roles) {
        UserDetailImpl userDetail = getCurrentUser();
        userDetail.setCurrentOrgId(currentOrgId);
        userDetail.setRoles(roles == null ? List.of() : roles);
        long ttlSeconds = redisTemplate.getExpire(RedisConstants.LOGIN_USER + userDetail.getId(), TimeUnit.SECONDS);
        if (ttlSeconds <= 0) {
            ttlSeconds = jwtUtil.getExpirationSeconds();
        }
        cacheLoginSession(userDetail, ttlSeconds);
    }

    @Override
    public UserDetailImpl getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        Object principal = authentication.getPrincipal();
        if (!(principal instanceof UserDetailImpl userDetail)) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return userDetail;
    }

    @Override
    public Long getCurrentUserId() {
        return getCurrentUser().getId();
    }

    private SessionLoginResultBO doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();

        String token = jwtUtil.generateToken(userId);
        long sessionTtlSeconds = jwtUtil.getRemainingTimeSeconds(token);

        currentOrganizationService.refreshCurrentOrgTtl(userId, sessionTtlSeconds);
        cacheLoginSession(userDetail, sessionTtlSeconds);

        return SessionLoginResultBO.builder()
                .userId(userId)
                .token(token)
                .roles(userDetail.getRoles())
                .currentOrgId(userDetail.getCurrentOrgId())
                .build();
    }

    private void cacheLoginSession(UserDetailImpl userDetail, long ttlSeconds) {
        LoginSessionCacheBO sessionCacheBO = new LoginSessionCacheBO();
        BeanUtils.copyProperties(userDetail, sessionCacheBO);
        sessionCacheBO.setUserId(userDetail.getId());

        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER + userDetail.getId(),
                JSON.toJSONString(sessionCacheBO),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }
}

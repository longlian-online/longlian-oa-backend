package online.longlian.app.service.user.impl;

import com.alibaba.fastjson2.JSON;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.security.UserDetailsServiceImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.pojo.bo.LoginSessionCacheBO;
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

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final RedisBlacklistUtil redisBlacklistUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final CurrentOrganizationService currentOrganizationService;
    private final UserDetailsServiceImpl userDetailsService;

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
        redisBlacklistUtil.addToBlacklist(params.getToken(), remainingSeconds);

        redisTemplate.delete(RedisConstants.LOGIN_USER + params.getUserId());
        currentOrganizationService.clearCurrentOrg(params.getUserId());
    }

    @Override
    public void refreshLoginSession(Long userId) {
        UserDetailImpl userDetail = userDetailsService.loadUserById(userId);
        cacheLoginSession(userDetail, jwtUtil.getExpirationSeconds());
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

        cacheLoginSession(userDetail, sessionTtlSeconds);
        currentOrganizationService.initializeCurrentOrg(userId, sessionTtlSeconds);

        return SessionLoginResultBO.builder()
                .userId(userId)
                .token(token)
                .roles(userDetail.getRoles())
                .defaultOrgId(userDetail.getDefaultOrgId())
                .build();
    }

    private void cacheLoginSession(UserDetailImpl userDetail, long ttlSeconds) {
        LoginSessionCacheBO sessionCacheBO = new LoginSessionCacheBO();
        BeanUtils.copyProperties(userDetail, sessionCacheBO);

        redisTemplate.opsForValue().set(
                RedisConstants.LOGIN_USER + userDetail.getId(),
                JSON.toJSONString(sessionCacheBO),
                ttlSeconds,
                TimeUnit.SECONDS
        );
    }
}

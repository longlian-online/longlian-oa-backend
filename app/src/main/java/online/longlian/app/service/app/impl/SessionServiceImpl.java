package online.longlian.app.service.app.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.TokenType;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final OrganizationMembershipService organizationMembershipService;

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
        tokenBlacklistService.addToBlacklist(params.getToken(), TokenType.User, params.getUserId(), "用户登出", remainingSeconds);

        redisTemplate.delete(RedisConstants.LOGIN_USER + params.getUserId());
    }

    @Override
    public void clearUserSessionCache(Long userId) {
        redisTemplate.delete(RedisConstants.LOGIN_USER + userId);
    }

    private SessionLoginResultBO doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();

        String token = jwtUtil.generateToken(userId, TokenType.User.name().toLowerCase());
        long sessionTtlSeconds = jwtUtil.getRemainingTimeSeconds(token);

        OrganizationMember suggested = organizationMembershipService.suggestForLogin(userId);
        cacheLoginSession(userDetail, sessionTtlSeconds);

        return SessionLoginResultBO.builder()
                .userId(userId)
                .token(token)
                .roles(StringUtils.hasText(suggested.getOrgRole()) ? List.of(suggested.getOrgRole()) : List.of())
                .currentOrgId(suggested.getOrgId())
                .build();
    }

    private void cacheLoginSession(UserDetailImpl userDetail, long ttlSeconds) {
        try {
            LoginSessionCacheBO sessionCacheBO = new LoginSessionCacheBO();
            BeanUtils.copyProperties(userDetail, sessionCacheBO);
            sessionCacheBO.setUserId(userDetail.getId());

            redisTemplate.opsForValue().set(
                    RedisConstants.LOGIN_USER + userDetail.getId(),
                    sessionCacheBO,
                    ttlSeconds,
                    TimeUnit.SECONDS
            );
        } catch (Exception e) {
            log.warn("缓存用户登录会话失败，userId={}", userDetail.getId(), e);
        }
    }
}

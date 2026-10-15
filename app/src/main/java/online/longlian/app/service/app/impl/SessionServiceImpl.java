package online.longlian.app.service.app.impl;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.EmailCodeAuthenticationToken;
import online.longlian.app.common.security.MyUsernamePasswordAuthenticationToken;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.security.UserDetailsServiceImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.bo.app.SessionRefreshResultBO;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.app.SessionService;
import online.longlian.app.service.common.DefaultOrganization;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.impl.RefreshSessionStore;
import online.longlian.app.service.impl.RefreshSessionStore.AccountUnavailableException;
import online.longlian.app.service.impl.RefreshSessionStore.CreatedRefresh;
import online.longlian.app.service.impl.RefreshSessionStore.RefreshExchange;
import online.longlian.app.service.impl.RefreshSessionStore.RefreshStatus;
import online.longlian.common.enumeration.TokenType;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class SessionServiceImpl implements SessionService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final JwtUtil jwtUtil;
    private final OrganizationMembershipService organizationMembershipService;
    private final RefreshSessionStore refreshSessionStore;
    private final UserDetailsServiceImpl userDetailsService;
    private final Clock clock;

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
        Claims claims = jwtUtil.parseAllowingExpiry(params.getToken());
        if (claims == null) {
            return;
        }
        String sessionId = claims.get("sid", String.class);
        if (StringUtils.hasText(sessionId)) {
            refreshSessionStore.deleteOne(sessionId, params.getUserId());
        }
    }

    @Override
    public SessionRefreshResultBO refresh(String refreshToken) {
        Duration ttl = Duration.ofSeconds(jwtUtil.getRefreshExpirationSeconds());
        RefreshStatus status = refreshSessionStore.exchange(
                refreshToken,
                clock.millis(),
                jwtUtil.getRefreshGraceSeconds() * 1000L,
                ttl,
                this::issueAccess
        );
        if (status.kind() != RefreshStatus.Kind.EXCHANGED || status.exchange() == null) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return SessionRefreshResultBO.builder()
                .token(status.exchange().accessToken())
                .refreshToken(status.exchange().refreshToken())
                .build();
    }

    @Override
    public void revokeRefreshSessions(Long userId) {
        refreshSessionStore.revoke(userId, clock.millis(), Duration.ofSeconds(jwtUtil.getRefreshExpirationSeconds()));
    }

    @Override
    public void clearUserSessionCache(Long userId) {
        redisTemplate.delete(RedisConstants.LOGIN_USER + userId);
    }

    private SessionLoginResultBO doLogin(Authentication authentication) {
        UserDetailImpl userDetail = (UserDetailImpl) authentication.getPrincipal();
        Long userId = userDetail.getId();
        CreatedRefresh created = refreshSessionStore.create(
                userId,
                clock.millis(),
                Duration.ofSeconds(jwtUtil.getRefreshExpirationSeconds()),
                this::newRefreshToken
        );
        String token = jwtUtil.generateAccessToken(userId, created.sessionId());

        DefaultOrganization defaultOrganization = organizationMembershipService.findDefault(userId);
        cacheLoginSession(userDetail, jwtUtil.getAccessExpirationSeconds());

        return SessionLoginResultBO.builder()
                .userId(userId)
                .token(token)
                .refreshToken(created.refreshToken())
                .role(StringUtils.hasText(defaultOrganization.orgRole())
                        ? defaultOrganization.orgRole() : null)
                .defaultOrgId(defaultOrganization.orgId())
                .build();
    }

    private RefreshExchange issueAccess(long userId, String sessionId) {
        UserDetailImpl userDetail;
        try {
            userDetail = (UserDetailImpl) userDetailsService.loadUserById(userId);
        } catch (AppException e) {
            throw new AccountUnavailableException();
        }
        if (!userDetail.isEnabled()) {
            throw new AccountUnavailableException();
        }
        cacheLoginSession(userDetail, jwtUtil.getAccessExpirationSeconds());
        return new RefreshExchange(jwtUtil.generateAccessToken(userId, sessionId), newRefreshToken());
    }

    private String newRefreshToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
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

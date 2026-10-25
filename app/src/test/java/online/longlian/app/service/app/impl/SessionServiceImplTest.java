package online.longlian.app.service.app.impl;

import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.security.UserDetailsServiceImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.common.DefaultOrganization;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.app.service.impl.RefreshSessionStore;
import online.longlian.app.service.impl.RefreshSessionStore.CreatedRefresh;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TokenType;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.lang.reflect.Field;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private OrganizationMembershipService organizationMembershipService;
    @Mock
    private RefreshSessionStore refreshSessionStore;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private Clock clock;
    @Mock
    private Claims claims;

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(tokenBlacklistService, authenticationManager, redisTemplate, jwtUtil,
                organizationMembershipService, refreshSessionStore, userDetailsService, clock);
    }

    @Test
    void loginByPwdReturnsDefaultOrgWithoutCachingRoles() {
        UserDetailImpl user = UserDetailImpl.builder().id(7L).username("user").status(Status.ENABLED).build();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(clock.millis()).thenReturn(10L);
        when(jwtUtil.getRefreshExpirationSeconds()).thenReturn(120L);
        when(jwtUtil.getUserAccessExpirationSeconds()).thenReturn(15L);
        when(refreshSessionStore.create(eq(7L), eq(10L), eq(Duration.ofSeconds(120)), any())).thenAnswer(invocation -> {
            Supplier<String> tokens = invocation.getArgument(3);
            return new CreatedRefresh("sid-1", tokens.get());
        });
        when(jwtUtil.generateAccessToken(7L, "sid-1")).thenReturn("token");
        when(organizationMembershipService.findDefault(7L)).thenReturn(new DefaultOrganization(11L, "ORG_ADMIN"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SessionLoginResultBO result = service.loginByPwd(SessionLoginByPwdParamsBO.builder()
                .username("user")
                .password("123456")
                .build());

        assertThat(result.getDefaultOrgId()).isEqualTo(11L);
        assertThat(result.getRole()).isEqualTo("ORG_ADMIN");
        assertThat(result.getToken()).isEqualTo("token");
        assertThat(result.getRefreshToken()).isNotBlank();
        ArgumentCaptor<LoginSessionCacheBO> cached = ArgumentCaptor.forClass(LoginSessionCacheBO.class);
        verify(valueOperations).set(eq(RedisConstants.LOGIN_USER + 7L), cached.capture(), eq(15L), eq(TimeUnit.SECONDS));
        assertThat(cached.getValue().getUserId()).isEqualTo(7L);
        assertThat(cached.getValue().getUsername()).isEqualTo("user");
        assertThat(LoginSessionCacheBO.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("role", "roles", "permissions", "currentOrgId");
        verify(organizationMembershipService).findDefault(7L);
    }

    @Test
    void loginByPwdOmitsBlankDefaultRole() {
        UserDetailImpl user = UserDetailImpl.builder().id(7L).username("user").status(Status.ENABLED).build();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(clock.millis()).thenReturn(10L);
        when(jwtUtil.getRefreshExpirationSeconds()).thenReturn(120L);
        when(jwtUtil.getUserAccessExpirationSeconds()).thenReturn(15L);
        when(refreshSessionStore.create(eq(7L), eq(10L), eq(Duration.ofSeconds(120)), any()))
                .thenReturn(new CreatedRefresh("sid-1", "refresh"));
        when(jwtUtil.generateAccessToken(7L, "sid-1")).thenReturn("token");
        when(organizationMembershipService.findDefault(7L)).thenReturn(new DefaultOrganization(11L, " "));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SessionLoginResultBO result = service.loginByPwd(SessionLoginByPwdParamsBO.builder()
                .username("user").password("123456").build());

        assertThat(result.getRole()).isNull();
        assertThat(result.getDefaultOrgId()).isEqualTo(11L);
        assertThat(result.getRefreshToken()).isEqualTo("refresh");
    }

    @Test
    void logoutRevokesOnlyThatDevice() {
        when(jwtUtil.getRemainingTimeSeconds("tok")).thenReturn(30L);
        when(jwtUtil.parseAllowingExpiry("tok")).thenReturn(claims);
        when(claims.get("sid", String.class)).thenReturn("sid-1");

        service.logout(SessionLogoutParamsBO.builder().userId(7L).token("tok").build());

        verify(tokenBlacklistService).addToBlacklist("tok", TokenType.User, 7L, "用户登出", 30L);
        verify(refreshSessionStore).deleteOne("sid-1", 7L);
        verify(redisTemplate, never()).delete(anyString());
    }

    @Test
    void logoutRejectsMissingTokenAndIgnoresUnreadableToken() {
        assertThatThrownBy(() -> service.logout(SessionLogoutParamsBO.builder().userId(7L).token(null).build()))
                .isInstanceOf(AppException.class);

        when(jwtUtil.getRemainingTimeSeconds("bad")).thenReturn(0L);
        when(jwtUtil.parseAllowingExpiry("bad")).thenReturn(null);
        service.logout(SessionLogoutParamsBO.builder().userId(7L).token("bad").build());

        verify(refreshSessionStore, never()).deleteOne(anyString(), eq(7L));
    }

    @Test
    void loginStillSucceedsWhenProfileCacheWriteFails() {
        UserDetailImpl user = UserDetailImpl.builder().id(7L).username("user").status(Status.ENABLED).build();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(clock.millis()).thenReturn(10L);
        when(jwtUtil.getRefreshExpirationSeconds()).thenReturn(120L);
        when(jwtUtil.getUserAccessExpirationSeconds()).thenReturn(15L);
        when(refreshSessionStore.create(eq(7L), eq(10L), eq(Duration.ofSeconds(120)), any()))
                .thenReturn(new CreatedRefresh("sid-1", "refresh"));
        when(jwtUtil.generateAccessToken(7L, "sid-1")).thenReturn("token");
        when(organizationMembershipService.findDefault(7L)).thenReturn(new DefaultOrganization(11L, "ORG_USER"));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        doThrow(new IllegalStateException("redis")).when(valueOperations)
                .set(any(), any(), eq(15L), eq(TimeUnit.SECONDS));

        SessionLoginResultBO result = service.loginByPwd(SessionLoginByPwdParamsBO.builder()
                .username("user").password("123456").build());

        assertThat(result.getToken()).isEqualTo("token");
    }

    @Test
    void refreshRejectsMissingUser() {
        when(clock.millis()).thenReturn(10L);
        when(jwtUtil.getRefreshExpirationSeconds()).thenReturn(120L);
        when(jwtUtil.getRefreshGraceSeconds()).thenReturn(1L);
        when(userDetailsService.loadUserById(7L)).thenThrow(new AppException(ResultCode.USER_NOT_EXIT));
        when(refreshSessionStore.exchange(any(), eq(10L), eq(1000L), eq(Duration.ofSeconds(120)), any()))
                .thenAnswer(invocation -> {
                    invocation.getArgument(4, RefreshSessionStore.RefreshIssuer.class).issue(7L, "sid-1");
                    return RefreshSessionStore.RefreshStatus.revoked();
                });

        assertThatThrownBy(() -> service.refresh("refresh"))
                .isInstanceOf(RefreshSessionStore.AccountUnavailableException.class);
    }
}

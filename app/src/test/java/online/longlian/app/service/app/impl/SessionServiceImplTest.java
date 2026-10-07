package online.longlian.app.service.app.impl;

import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import online.longlian.common.enumeration.TokenType;
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
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
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

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(tokenBlacklistService, authenticationManager,
                redisTemplate, jwtUtil, organizationMembershipService);
    }

    @Test
    void loginByPwdReturnsSuggestedOrgWithoutCachingRoles() {
        UserDetailImpl user = UserDetailImpl.builder().id(7L).username("user").status(Status.ENABLED).build();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtUtil.generateToken(7L, TokenType.User.name().toLowerCase())).thenReturn("token");
        when(jwtUtil.getRemainingTimeSeconds("token")).thenReturn(120L);
        when(organizationMembershipService.suggestForLogin(7L)).thenReturn(
                OrganizationMember.builder().id(3L).orgId(11L).orgRole("ORG_ADMIN").build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SessionLoginResultBO result = service.loginByPwd(SessionLoginByPwdParamsBO.builder()
                .username("user")
                .password("123456")
                .build());

        assertThat(result.getCurrentOrgId()).isEqualTo(11L);
        assertThat(result.getRoles()).containsExactly("ORG_ADMIN");
        ArgumentCaptor<LoginSessionCacheBO> cached = ArgumentCaptor.forClass(LoginSessionCacheBO.class);
        verify(valueOperations).set(eq(RedisConstants.LOGIN_USER + 7L), cached.capture(), eq(120L), eq(TimeUnit.SECONDS));
        assertThat(cached.getValue().getUserId()).isEqualTo(7L);
        assertThat(cached.getValue().getUsername()).isEqualTo("user");
        assertThat(LoginSessionCacheBO.class.getDeclaredFields())
                .extracting(Field::getName)
                .doesNotContain("roles", "permissions", "currentOrgId");
        verify(organizationMembershipService).suggestForLogin(7L);
    }
    @Test
    void loginByPwdOmitsBlankSuggestedRole() {
        UserDetailImpl user = UserDetailImpl.builder().id(7L).username("user").status(Status.ENABLED).build();
        when(authenticationManager.authenticate(any())).thenReturn(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
        when(jwtUtil.generateToken(7L, TokenType.User.name().toLowerCase())).thenReturn("token");
        when(jwtUtil.getRemainingTimeSeconds("token")).thenReturn(120L);
        when(organizationMembershipService.suggestForLogin(7L)).thenReturn(
                OrganizationMember.builder().id(3L).orgId(11L).orgRole(" ").build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        SessionLoginResultBO result = service.loginByPwd(SessionLoginByPwdParamsBO.builder()
                .username("user").password("123456").build());

        assertThat(result.getRoles()).isEmpty();
        assertThat(result.getCurrentOrgId()).isEqualTo(11L);
    }


    @Test
    void logoutDeletesOnlyLoginCache() {
        when(jwtUtil.getRemainingTimeSeconds("tok")).thenReturn(30L);

        service.logout(SessionLogoutParamsBO.builder().userId(7L).token("tok").build());

        verify(tokenBlacklistService).addToBlacklist("tok", TokenType.User, 7L, "用户登出", 30L);
        verify(redisTemplate, times(1)).delete(RedisConstants.LOGIN_USER + 7L);
    }
}

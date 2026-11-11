package online.longlian.app.common.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.AuthenticationStrategy;
import online.longlian.app.common.security.RequestAuthenticationException;
import online.longlian.app.common.security.UserAuthenticationStrategy;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.bo.common.McpApiKeyIdentityResultBO;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.app.service.app.UserOrgApiKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JwtAuthenticationFilterTest {
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final TokenBlacklistService blacklist = mock(TokenBlacklistService.class);
    private final AuthenticationEntryPoint entryPoint = mock(AuthenticationEntryPoint.class);
    private final AuthenticationStrategy strategy = mock(AuthenticationStrategy.class);
    private final UserAuthenticationStrategy userStrategy = mock(UserAuthenticationStrategy.class);
    private final UserOrgApiKeyService apiKeyService = mock(UserOrgApiKeyService.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/admins/");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        when(strategy.supportedType()).thenReturn("admin");
        when(userStrategy.supportedType()).thenReturn("user");
        filter = new JwtAuthenticationFilter(jwt, entryPoint, blacklist, List.of(strategy, userStrategy), apiKeyService);
        filter.init();
        clearInvocations(strategy, userStrategy);
        request.addHeader("Authorization", "Bearer token");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** 过期 token 保留原始原因，且不会访问数据库。 */
    @Test
    void shouldRejectExpiredTokenBeforeBlacklistLookup() throws Exception {
        ExpiredJwtException cause = new ExpiredJwtException(null, null, "private token details");
        when(jwt.parseToken("token")).thenThrow(cause);
        filter.doFilter(request, response, chain);
        assertThat(failure().getCause()).isSameAs(cause);
        assertThat(failure().getMessage()).doesNotContain("private token details");
        verifyNoInteractions(blacklist, chain);
    }

    /** 无效签名和格式错误的 token 会被拒绝，且不会执行 SQL 查询。 */
    @Test
    void shouldRejectInvalidTokenWithoutLeakingParserMessage() throws Exception {
        when(jwt.parseToken("token")).thenThrow(new MalformedJwtException("private token details"));
        filter.doFilter(request, response, chain);
        assertThat(failure().getMessage()).doesNotContain("private token details");
        verifyNoInteractions(blacklist, chain);
    }

    /** 吊销检查失败时必须清除之前残留的安全上下文。 */
    @Test
    void shouldRejectRevokedTokenAndClearContext() throws Exception {
        validClaims();
        when(blacklist.isBlacklisted("token")).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("old", null));
        filter.doFilter(request, response, chain);
        assertThat(((RequestAuthenticationException) failure()).getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(chain);
    }

    /** subject 不是数字时只按无效凭证处理，不应访问黑名单存储。 */
    @Test
    void shouldRejectMalformedSubjectWithoutBlacklistLookup() throws Exception {
        var claims = Jwts.claims().setSubject("not-a-number");
        claims.put("type", "admin");
        when(jwt.parseToken("token")).thenReturn(claims);
        filter.doFilter(request, response, chain);
        assertThat(failure().getMessage()).isEqualTo("登录凭证无效");
        verifyNoInteractions(blacklist, chain);
    }

    /** Token 类型字段格式错误时按无效凭证处理，不应进入策略层。 */
    @Test
    void shouldRejectMalformedTypeWithoutAuthentication() throws Exception {
        Claims claims = mock(Claims.class);
        when(claims.getSubject()).thenReturn("1");
        when(claims.get("type", String.class)).thenThrow(new MalformedJwtException("private token details"));
        when(jwt.parseToken("token")).thenReturn(claims);
        filter.doFilter(request, response, chain);
        assertThat(failure().getMessage()).isEqualTo("登录凭证无效");
        verifyNoInteractions(blacklist, chain);
        verify(strategy, never()).authenticate(anyLong());
    }

    /** 未配置的 Token 类型必须被拒绝，避免绕过认证策略。 */
    @Test
    void shouldRejectUnsupportedType() throws Exception {
        var claims = Jwts.claims().setSubject("1");
        claims.put("type", "unsupported");
        when(jwt.parseToken("token")).thenReturn(claims);
        filter.doFilter(request, response, chain);
        assertThat(failure().getMessage()).isEqualTo("登录凭证无效");
        verify(blacklist).isBlacklisted("token");
        verifyNoInteractions(chain);
    }

    /** 业务授权错误保留业务码和可公开的提示信息。 */
    @Test
    void shouldPreserveBusinessFailure() throws Exception {
        validClaims();
        AppException cause = new AppException(ResultCode.UNAUTHORIZED_OPERATION);
        when(strategy.authenticate(1L)).thenThrow(cause);
        filter.doFilter(request, response, chain);
        RequestAuthenticationException failure = (RequestAuthenticationException) failure();
        assertThat(failure.getCode()).isEqualTo(cause.getCode());
        assertThat(failure.getMessage()).isEqualTo(cause.getMsg());
        assertThat(failure.getCause()).isSameAs(cause);
    }

    /** 策略层的认证异常应直接交给认证入口处理。 */
    @Test
    void shouldPreserveAuthenticationFailure() throws Exception {
        validClaims();
        AuthenticationException cause = new BadCredentialsException("凭证无效");
        when(strategy.authenticate(1L)).thenThrow(cause);
        filter.doFilter(request, response, chain);
        assertThat(failure()).isSameAs(cause);
    }

    /** 基础设施故障不能伪装成无效凭证。 */
    @Test
    void shouldReportInfrastructureFailureWithoutDetails() throws Exception {
        validClaims();
        when(blacklist.isBlacklisted("token"))
                .thenThrow(new DataAccessResourceFailureException("sql secret"));
        filter.doFilter(request, response, chain);
        RequestAuthenticationException failure = (RequestAuthenticationException) failure();
        assertThat(failure.getCode()).isEqualTo(ResultCode.FAIL.getCode());
        assertThat(failure.getMessage()).doesNotContain("sql secret");
    }

    /** 认证成功后请求只继续执行一次。 */
    @Test
    void shouldContinueAuthenticatedRequest() throws Exception {
        validClaims();
        var authentication = new UsernamePasswordAuthenticationToken("admin", null, List.of());
        when(strategy.authenticate(1L)).thenReturn(authentication);
        filter.doFilter(request, response, chain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        verify(chain).doFilter(request, response);
        verifyNoInteractions(entryPoint);
    }

    private void validClaims() {
        var claims = Jwts.claims().setSubject("1");
        claims.put("type", "admin");
        when(jwt.parseToken("token")).thenReturn(claims);
    }

    /** 系统信息无需认证，即使带无效 Token 也不应解析凭证或查询黑名单。 */
    @Test
    void shouldSkipAuthenticationForSystemInfo() throws Exception {
        request.setRequestURI("/common/system/info");
        request.setServletPath("/common/system/info");

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
        verifyNoInteractions(jwt, blacklist, entryPoint);
        verify(strategy, never()).authenticate(anyLong());
    }

    /** 免鉴权范围限于系统信息 GET，避免放行写入或其他系统路径。 */
    @Test
    void shouldKeepOtherSystemRequestsAuthenticated() {
        request.setServletPath("/common/system/info");
        request.setMethod("POST");
        assertThat(filter.shouldNotFilter(request)).isFalse();

        request.setMethod("GET");
        request.setServletPath("/common/system/private");
        assertThat(filter.shouldNotFilter(request)).isFalse();
    }

    /** API Key 复用已有用户身份，绑定范围仅存本次请求，不把 Key 或组织写入用户对象。 */
    @Test
    void shouldAuthenticateApiKeyAsExistingUserAndBindRequestScope() throws Exception {
        apiKeyRequest();
        request.addHeader("X-Org-Id", " 2 ");
        when(apiKeyService.authenticate("ll_mcp_key")).thenReturn(new McpApiKeyIdentityResultBO(1L, 2L, 3L));
        UserDetailImpl user = UserDetailImpl.builder().id(1L).build();
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of());
        when(userStrategy.authenticate(1L)).thenReturn(authentication);

        filter.doFilter(request, response, chain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(authentication);
        assertThat(authentication.getPrincipal()).isSameAs(user);
        assertThat(authentication.getCredentials()).isNull();
        assertThat(request.getAttribute(SessionContext.class.getName())).isEqualTo(new SessionContext(1L, 2L));
        verify(chain).doFilter(request, response);
        verifyNoInteractions(entryPoint);
    }

    /** 请求头不能覆盖 Key 固定的组织，冲突时清除认证并返回 MCP 协议层 403。 */
    @Test
    void shouldRejectConflictingApiKeyOrganization() throws Exception {
        apiKeyRequest();
        request.addHeader("X-Org-Id", "9");
        when(apiKeyService.authenticate("ll_mcp_key")).thenReturn(new McpApiKeyIdentityResultBO(1L, 2L, 3L));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("old", null));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        assertThat(request.getAttribute(SessionContext.class.getName())).isNull();
        verifyNoInteractions(userStrategy, chain, entryPoint);
    }

    /** 成员或组织停用仍保留 API Key 原有的 HTTP 403 约定。 */
    @Test
    void shouldRejectSuspendedApiKeyMembership() throws Exception {
        apiKeyRequest();
        when(apiKeyService.authenticate("ll_mcp_key")).thenThrow(new AppException(ResultCode.UNAUTHORIZED_OPERATION));

        filter.doFilter(request, response, chain);

        assertThat(response.getStatus()).isEqualTo(403);
        verifyNoInteractions(userStrategy, chain, entryPoint);
    }

    /** 已作废的 Key 直接拒绝，不能回退到 JWT 或使用之前残留的认证上下文。 */
    @Test
    void shouldRejectInvalidApiKeyWithoutJwtFallback() throws Exception {
        apiKeyRequest();
        when(apiKeyService.authenticate("ll_mcp_key")).thenThrow(new AppException(ResultCode.UNAUTHORIZED));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("old", null));

        filter.doFilter(request, response, chain);

        assertThat(((RequestAuthenticationException) failure()).getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(jwt, blacklist, userStrategy, chain);
    }

    /** 统一入口仍将 Key 限制在 MCP，不能借标准用户身份认证网页 REST 请求。 */
    @Test
    void shouldRejectApiKeyOutsideMcpWithoutTryingOtherAuthentication() throws Exception {
        request.removeHeader("Authorization");
        request.addHeader("Authorization", "Bearer ll_mcp_key");
        request.setServletPath("/app/user/");

        filter.doFilter(request, response, chain);

        assertThat(failure().getMessage()).isEqualTo("登录凭证无效");
        verifyNoInteractions(apiKeyService, jwt, blacklist, strategy, userStrategy, chain);
    }

    /** API Key 数据库故障沿用公共认证失败处理，响应中不携带 SQL 信息。 */
    @Test
    void shouldReportApiKeyInfrastructureFailureWithoutDetails() throws Exception {
        apiKeyRequest();
        when(apiKeyService.authenticate("ll_mcp_key")).thenThrow(new DataAccessResourceFailureException("private SQL"));

        filter.doFilter(request, response, chain);

        RequestAuthenticationException failure = (RequestAuthenticationException) failure();
        assertThat(failure.getCode()).isEqualTo(ResultCode.FAIL.getCode());
        assertThat(failure.getMessage()).doesNotContain("private SQL");
        verifyNoInteractions(userStrategy, chain);
    }

    /** 认证用户失败时不发布绑定组织；仍使用原有用户策略返回的失败原因。 */
    @Test
    void shouldNotPublishBoundScopeWhenUserAuthenticationFails() throws Exception {
        apiKeyRequest();
        when(apiKeyService.authenticate("ll_mcp_key")).thenReturn(new McpApiKeyIdentityResultBO(1L, 2L, 3L));
        when(userStrategy.authenticate(1L)).thenThrow(new AppException(ResultCode.UNAUTHORIZED));

        filter.doFilter(request, response, chain);

        assertThat(((RequestAuthenticationException) failure()).getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(request.getAttribute(SessionContext.class.getName())).isNull();
        verifyNoInteractions(chain);
    }

    private void apiKeyRequest() {
        request.setMethod("POST");
        request.setRequestURI("/mcp");
        request.setServletPath("/mcp");
        request.removeHeader("Authorization");
        request.addHeader("Authorization", "Bearer ll_mcp_key");
    }

    private AuthenticationException failure() throws Exception {
        var captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(entryPoint).commence(eq(request), eq(response), captor.capture());
        return captor.getValue();
    }
}

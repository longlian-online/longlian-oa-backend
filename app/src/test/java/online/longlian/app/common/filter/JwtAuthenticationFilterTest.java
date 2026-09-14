package online.longlian.app.common.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import jakarta.servlet.FilterChain;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.AuthenticationStrategy;
import online.longlian.app.common.security.RequestAuthenticationException;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.service.TokenBlacklistService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
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
    private final FilterChain chain = mock(FilterChain.class);
    private final MockHttpServletRequest request = new MockHttpServletRequest("GET", "/admin/admins/");
    private final MockHttpServletResponse response = new MockHttpServletResponse();
    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        when(strategy.supportedType()).thenReturn("admin");
        filter = new JwtAuthenticationFilter(jwt, entryPoint, blacklist, List.of(strategy));
        filter.init();
        request.addHeader("Authorization", "Bearer token");
    }

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    /** Expired tokens retain their cause and do not reach the database. */
    @Test
    void shouldRejectExpiredTokenBeforeBlacklistLookup() throws Exception {
        ExpiredJwtException cause = new ExpiredJwtException(null, null, "private token details");
        when(jwt.parseToken("token")).thenThrow(cause);
        filter.doFilter(request, response, chain);
        assertThat(failure().getCause()).isSameAs(cause);
        assertThat(failure().getMessage()).doesNotContain("private token details");
        verifyNoInteractions(blacklist, chain);
    }

    /** Invalid signatures and malformed tokens are rejected without SQL queries. */
    @Test
    void shouldRejectInvalidTokenWithoutLeakingParserMessage() throws Exception {
        when(jwt.parseToken("token")).thenThrow(new MalformedJwtException("private token details"));
        filter.doFilter(request, response, chain);
        assertThat(failure().getMessage()).doesNotContain("private token details");
        verifyNoInteractions(blacklist, chain);
    }

    /** Revocation failures clear any previous security context. */
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

    /** Business authorization errors preserve their code and public message. */
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

    /** Infrastructure failures must not masquerade as invalid credentials. */
    @Test
    void shouldReportInfrastructureFailureWithoutDetails() throws Exception {
        validClaims();
        when(blacklist.isBlacklisted("token")).thenThrow(new IllegalStateException("sql secret"));
        filter.doFilter(request, response, chain);
        RequestAuthenticationException failure = (RequestAuthenticationException) failure();
        assertThat(failure.getCode()).isEqualTo(ResultCode.FAIL.getCode());
        assertThat(failure.getMessage()).doesNotContain("sql secret");
    }

    /** Successful authentication continues the request exactly once. */
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

    private AuthenticationException failure() throws Exception {
        var captor = ArgumentCaptor.forClass(AuthenticationException.class);
        verify(entryPoint).commence(eq(request), eq(response), captor.capture());
        return captor.getValue();
    }
}

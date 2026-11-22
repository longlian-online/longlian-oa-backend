package online.longlian.app.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.OpenApiConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.common.security.RequestAuthenticationException;
import online.longlian.app.pojo.bo.open.OpenApiKeyAuthResultBO;
import online.longlian.app.service.open.OpenApiKeyService;
import online.longlian.app.service.open.OpenApiRateLimitService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * /open/** 的密钥认证过滤器。只认 X-Api-Key，独立头不与 Bearer JWT 冲突；
 * 未带密钥时放行，由安全链按未认证处理。认证失败与限流的拒绝都走站内认证入口点，
 * 与 JwtAuthenticationFilter 同款：HTTP 200 + Result 信封。
 * 认证通过后先过按密钥限流，再放行到业务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String API_KEY_HEADER = "X-Api-Key";

    private static final String OPEN_PATH_PREFIX = "/open/";

    private final OpenApiKeyService openApiKeyService;
    private final OpenApiRateLimitService openApiRateLimitService;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getServletPath().startsWith(OPEN_PATH_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(API_KEY_HEADER);
        if (!StringUtils.hasText(apiKey)) {
            filterChain.doFilter(request, response);
            return;
        }

        OpenApiKeyAuthResultBO auth;
        try {
            auth = openApiKeyService.authenticate(apiKey);
            if (!openApiRateLimitService.tryAcquire(auth.keyId())) {
                throw new AppException(ResultCode.OPEN_RATE_LIMITED);
            }
        } catch (AppException e) {
            SecurityContextHolder.clearContext();
            authenticationEntryPoint.commence(request, response,
                    new RequestAuthenticationException(e.getCode(), e.getMsg(), e));
            return;
        }

        OpenApiPrincipal principal = new OpenApiPrincipal(auth.keyId(), auth.userId(), auth.orgId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(OpenApiConstants.OPEN_API_AUTHORITY))));

        openApiKeyService.touchLastUsedAsync(auth.keyId());

        filterChain.doFilter(request, response);
    }
}

package online.longlian.app.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.OpenAppException;
import online.longlian.app.common.handler.OpenApiResponseWriter;
import online.longlian.app.common.metrics.ApiResultMetrics;
import online.longlian.app.common.result.OpenResultCode;
import online.longlian.app.common.constants.OpenApiConstants;
import online.longlian.app.common.security.OpenApiPrincipal;
import online.longlian.app.pojo.bo.open.OpenApiKeyAuthResultBO;
import online.longlian.app.service.open.OpenApiKeyService;
import online.longlian.app.service.open.OpenApiRateLimitService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * /open/** 的密钥认证过滤器。只认 X-Api-Key，独立头不与 Bearer JWT 冲突；
 * 未带密钥时放行，由安全链按未认证处理（入口点按 /open 前缀写 401 开放信封）。
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
    private final ApiResultMetrics apiResultMetrics;

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
        } catch (OpenAppException e) {
            apiResultMetrics.record(request, e.getCode());
            OpenApiResponseWriter.write(response, OpenResultCode.UNAUTHENTICATED, e.getMsg());
            return;
        }

        if (!openApiRateLimitService.tryAcquire(auth.keyId())) {
            apiResultMetrics.record(request, OpenResultCode.RATE_LIMITED.getCode());
            OpenApiResponseWriter.write(response, OpenResultCode.RATE_LIMITED, "请求过于频繁，请稍后重试");
            return;
        }

        OpenApiPrincipal principal = new OpenApiPrincipal(auth.keyId(), auth.userId(), auth.orgId());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority(OpenApiConstants.OPEN_API_AUTHORITY))));

        openApiKeyService.touchLastUsedAsync(auth.keyId());

        filterChain.doFilter(request, response);
    }
}

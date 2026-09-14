package online.longlian.app.common.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.SecurityConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.AuthenticationStrategy;
import online.longlian.app.common.security.RequestAuthenticationException;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.service.TokenBlacklistService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final TokenBlacklistService tokenBlacklistService;
    private final List<AuthenticationStrategy> strategies;

    private Map<String, AuthenticationStrategy> strategyMap;

    @PostConstruct
    public void init() {
        strategyMap = strategies.stream()
                .collect(Collectors.toMap(AuthenticationStrategy::supportedType, Function.identity()));
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return SecurityConstants.getPermitAllMatchers().stream()
                .anyMatch(matcher -> matcher.matches(request));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            Claims claims = jwtUtil.parseToken(token);
            if (tokenBlacklistService.isBlacklisted(token)) {
                throw new RequestAuthenticationException(ResultCode.UNAUTHORIZED.getCode(), "登录凭证已撤销，请重新登录", null);
            }

            long subjectId = Long.parseLong(claims.getSubject());
            String type = claims.get("type", String.class);

            AuthenticationStrategy strategy = strategyMap.get(type);
            if (strategy == null) {
                throw new RequestAuthenticationException(ResultCode.UNAUTHORIZED.getCode(), "登录凭证无效", null);
            }

            Authentication authentication = strategy.authenticate(subjectId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (ExpiredJwtException e) {
            reject(request, response, new RequestAuthenticationException(ResultCode.UNAUTHORIZED.getCode(), "登录凭证已过期，请重新登录", e));
            return;
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, response, new RequestAuthenticationException(ResultCode.UNAUTHORIZED.getCode(), "登录凭证无效", e));
            return;
        } catch (AppException e) {
            reject(request, response, new RequestAuthenticationException(e.getCode(), e.getMsg(), e));
            return;
        } catch (AuthenticationException e) {
            reject(request, response, e);
            return;
        } catch (Exception e) {
            // 驱动异常可能包含 SQL 参数，不能记录带有 token 的异常文本。
            log.error("鉴权基础设施异常 | type={}", e.getClass().getName());
            reject(request, response, new RequestAuthenticationException(ResultCode.FAIL.getCode(), ResultCode.FAIL.getMsg(), e));
            return;
        }
        filterChain.doFilter(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response,
                        AuthenticationException exception) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        authenticationEntryPoint.commence(request, response, exception);
    }
}

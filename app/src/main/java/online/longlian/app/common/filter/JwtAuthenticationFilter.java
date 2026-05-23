package online.longlian.app.common.filter;

import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.SecurityConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.AuthenticationStrategy;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.service.TokenBlacklistService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.util.matcher.RequestMatcher;
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
        for (RequestMatcher matcher : SecurityConstants.getPermitAllMatchers()) {
            if (matcher.matches(request)) {
                return true;
            }
        }
        return false;
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
            if (tokenBlacklistService.isBlacklisted(token)) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }
            Claims claims = jwtUtil.parseTokenIfValid(token);
            if (claims == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            long subjectId = Long.parseLong(claims.getSubject());
            String type = claims.get("type", String.class);

            AuthenticationStrategy strategy = strategyMap.get(type);
            if (strategy == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            Authentication authentication = strategy.authenticate(subjectId);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response, null);
        }
    }
}

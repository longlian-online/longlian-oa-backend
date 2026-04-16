package online.longlian.app.common.filter;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthenticationEntryPoint authenticationEntryPoint;
    private final RedisBlacklistUtil redisBlacklistUtil;

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
        request.setAttribute(CommonConstants.CURRENT_TOKEN, token);
        try {
            if (redisBlacklistUtil.isInBlacklist(token)) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }
            Claims claims = jwtUtil.parseTokenIfValid(token);
            if (claims == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }
            long userId = Long.parseLong(claims.getSubject());

            Object cached = redisTemplate.opsForValue().get(RedisConstants.LOGIN_USER + userId);
            if (cached == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            UserDetailImpl userDetailImpl = JSON.parseObject(cached.toString(), UserDetailImpl.class);
            if (userDetailImpl == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetailImpl, null, userDetailImpl.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response, null);
        }
    }
}

package online.longlian.app.common.filter;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.ThreadLocalUtil;
import online.longlian.app.pojo.bo.UserBO;
import online.longlian.app.common.security.UserDetailImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Objects;

@RequiredArgsConstructor
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthenticationEntryPoint authenticationEntryPoint;

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
            // 校验 token
            if (!jwtUtil.validateToken(token)) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            Claims claims = jwtUtil.parseToken(token);
            long userId = Long.parseLong(claims.getSubject());

            // Redis 验证 token
            Object redisToken = redisTemplate.opsForValue().get(RedisConstants.TOKEN + userId);
            if (redisToken == null || !Objects.equals(redisToken.toString(), token)) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            String json = Objects.requireNonNull(redisTemplate.opsForValue().get(RedisConstants.LOGIN_USER + userId)).toString();
            UserDetailImpl userDetailImpl = JSON.parseObject(json, UserDetailImpl.class);
            if (userDetailImpl == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            // 设置 Spring Security 上下文
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetailImpl, null, userDetailImpl.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            UserBO userBO = new UserBO();
            BeanUtils.copyProperties(userDetailImpl, userBO);
            ThreadLocalUtil.setUserBO(userBO);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response, null);
        } finally {
            ThreadLocalUtil.removeUserBO();
        }
    }
}

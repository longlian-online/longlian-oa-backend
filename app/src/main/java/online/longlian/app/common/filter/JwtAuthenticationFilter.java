package online.longlian.app.common.filter;

import com.alibaba.fastjson2.JSON;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.pojo.bo.LoginSessionCacheBO;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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

            LoginSessionCacheBO sessionCacheBO = JSON.parseObject(cached.toString(), LoginSessionCacheBO.class);
            if (sessionCacheBO == null) {
                throw new BadCredentialsException(ResultCode.UNAUTHORIZED.getMsg());
            }

            UserDetailImpl userDetailImpl = buildUserDetail(sessionCacheBO);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetailImpl, null, userDetailImpl.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (Exception e) {
            authenticationEntryPoint.commence(request, response, null);
        }
    }

    private UserDetailImpl buildUserDetail(LoginSessionCacheBO sessionCacheBO) {
        UserDetailImpl userDetail = new UserDetailImpl();
        BeanUtils.copyProperties(sessionCacheBO, userDetail);
        userDetail.setId(sessionCacheBO.getUserId());

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (sessionCacheBO.getPermissions() != null) {
            authorities.addAll(sessionCacheBO.getPermissions().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList());
        }
        if (sessionCacheBO.getRoles() != null) {
            authorities.addAll(sessionCacheBO.getRoles().stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .toList());
        }
        userDetail.setAuthorities(new ArrayList<>(authorities));
        return userDetail;
    }
}

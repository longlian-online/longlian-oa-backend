package online.longlian.app.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.common.enumeration.TokenType;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserAuthenticationStrategy implements AuthenticationStrategy {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    public String supportedType() {
        return TokenType.User.name().toLowerCase();
    }

    @Override
    public Authentication authenticate(long subjectId) {
        UserDetailImpl userDetail = getCachedUserDetail(subjectId);
        if (userDetail == null) {
            userDetail = (UserDetailImpl) userDetailsService.loadUserById(subjectId);
        }
        if (userDetail == null || !userDetail.isEnabled()) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
    }

    /**
     * 用数据库中的认证版本校验 JWT。Redis 登录快照可能在事务提交后被旧读结果写回。
     */
    public Authentication authenticate(long subjectId, int presentedAuthVersion) {
        Authentication authentication = authenticate(subjectId);
        int current = userDetailsService.currentAuthVersion(subjectId);
        if (authentication.getPrincipal() instanceof UserDetailImpl user) {
            user.setAuthVersion(current);
        }
        if (presentedAuthVersion != current) {
            throw new RequestAuthenticationException(ResultCode.UNAUTHORIZED.getCode(), "登录凭证已撤销，请重新登录", null);
        }
        return authentication;
    }

    private UserDetailImpl getCachedUserDetail(Long userId) {
        try {
            Object cached = redisTemplate.opsForValue().get(RedisConstants.LOGIN_USER + userId);
            if (!(cached instanceof LoginSessionCacheBO sessionCacheBO)) {
                return null;
            }
            return buildUserDetail(sessionCacheBO);
        } catch (Exception e) {
            log.warn("读取用户登录缓存失败，userId={}", userId, e);
            return null;
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

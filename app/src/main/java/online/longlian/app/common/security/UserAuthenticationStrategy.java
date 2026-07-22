package online.longlian.app.common.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.TokenType;
import online.longlian.common.enumeration.Status;
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
    private final UserMapper userMapper;

    @Override
    public String supportedType() {
        return TokenType.User.name().toLowerCase();
    }

    @Override
    public Authentication authenticate(long subjectId) {
        ensureUserEnabled(subjectId);
        UserDetailImpl userDetail = getCachedUserDetail(subjectId);
        if (userDetail == null) {
            userDetail = (UserDetailImpl) userDetailsService.loadUserById(subjectId);
        }
        return new UsernamePasswordAuthenticationToken(userDetail, null, userDetail.getAuthorities());
    }

    private void ensureUserEnabled(long userId) {
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
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

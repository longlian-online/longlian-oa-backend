package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.entity.User;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAuthenticationStrategyTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @Mock
    private UserDetailsServiceImpl userDetailsService;

    @Mock
    private UserMapper userMapper;

    @Test
    void shouldLoadUserDetailsWhenLoginCacheIsMissing() {
        UserAuthenticationStrategy strategy = createStrategy();
        UserDetailImpl userDetail = UserDetailImpl.builder().id(1L).username("user").status(Status.ENABLED).build();
        when(userMapper.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("login:user:1")).thenReturn(null);
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetail);

        Authentication authentication = strategy.authenticate(1L);

        Assertions.assertSame(userDetail, authentication.getPrincipal());
        verify(userDetailsService).loadUserById(1L);
    }

    @Test
    void shouldLoadUserDetailsWhenLoginCacheCannotBeRead() {
        UserAuthenticationStrategy strategy = createStrategy();
        UserDetailImpl userDetail = UserDetailImpl.builder().id(1L).username("user").status(Status.ENABLED).build();
        when(userMapper.selectById(1L)).thenReturn(User.builder().status(Status.ENABLED).build());
        doThrow(new IllegalStateException("redis unavailable")).when(redisTemplate).opsForValue();
        when(userDetailsService.loadUserById(1L)).thenReturn(userDetail);

        Authentication authentication = strategy.authenticate(1L);

        Assertions.assertSame(userDetail, authentication.getPrincipal());
        verify(userDetailsService).loadUserById(1L);
    }

    @Test
    void shouldRejectDisabledUserBeforeLoadingCachedSession() {
        UserAuthenticationStrategy strategy = createStrategy();
        when(userMapper.selectById(1L)).thenReturn(User.builder().status(Status.DISABLED).build());

        Assertions.assertThrows(AppException.class, () -> strategy.authenticate(1L));
    }

    private UserAuthenticationStrategy createStrategy() {
        return new UserAuthenticationStrategy(redisTemplate, userDetailsService, userMapper);
    }
}

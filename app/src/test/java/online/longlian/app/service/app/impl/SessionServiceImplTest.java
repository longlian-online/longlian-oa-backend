package online.longlian.app.service.app.impl;

import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.common.LoginSessionCacheBO;
import online.longlian.app.service.TokenBlacklistService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private TokenBlacklistService tokenBlacklistService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private ValueOperations<String, Object> valueOperations;
    @Mock
    private JwtUtil jwtUtil;

    private SessionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SessionServiceImpl(tokenBlacklistService, authenticationManager, redisTemplate, jwtUtil);
    }

    @Test
    void refreshCurrentUserOrgUpdatesCachedSession() {
        LoginSessionCacheBO session = LoginSessionCacheBO.builder().sessionId("session-1").userId(7L).build();
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(RedisConstants.LOGIN_USER + "session-1")).thenReturn(session);
        when(redisTemplate.getExpire(RedisConstants.LOGIN_USER + "session-1", TimeUnit.SECONDS)).thenReturn(-1L);
        when(jwtUtil.getExpirationSeconds()).thenReturn(300L);

        service.refreshCurrentUserOrg("session-1", 7L, 11L, List.of("ORG_ADMIN"));

        verify(valueOperations).set(RedisConstants.LOGIN_USER + "session-1", session, 300L, TimeUnit.SECONDS);
    }

    @Test
    void refreshCurrentUserOrgRejectsMissingCachedSession() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThatThrownBy(() -> service.refreshCurrentUserOrg("session-1", 7L, 11L, List.of()))
                .isInstanceOf(AppException.class);
    }
}

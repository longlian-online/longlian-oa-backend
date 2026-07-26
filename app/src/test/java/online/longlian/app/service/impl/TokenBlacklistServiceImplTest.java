package online.longlian.app.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

    @Mock
    private TokenBlacklistMapper tokenBlacklistMapper;
    @Mock
    private JwtUtil jwtUtil;

    private final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneId.of("UTC"));
    private TokenBlacklistServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new TokenBlacklistServiceImpl(tokenBlacklistMapper, jwtUtil, clock);
    }

    @Test
    void addToBlacklist_expiredToken_skipsInsert() {
        service.addToBlacklist("token", TokenType.User, 1L, "logout", 0);
        verify(tokenBlacklistMapper, never()).insert(any(TokenBlacklist.class));
    }

    @Test
    void addToBlacklist_negativeExpire_skipsInsert() {
        service.addToBlacklist("token", TokenType.User, 1L, "logout", -5);
        verify(tokenBlacklistMapper, never()).insert(any(TokenBlacklist.class));
    }

    @Test
    void addToBlacklist_validToken_insertsRecord() {
        when(tokenBlacklistMapper.insert(any(TokenBlacklist.class))).thenReturn(1);

        service.addToBlacklist("token123", TokenType.User, 1L, "logout", 3600);

        verify(tokenBlacklistMapper).insert(any(TokenBlacklist.class));
    }

    @Test
    void isBlacklisted_directMatch_returnsTrue() {
        when(tokenBlacklistMapper.selectCount(any())).thenReturn(1L);

        assertThat(service.isBlacklisted("blacklisted-token")).isTrue();
    }

    @Test
    void isBlacklisted_noDirectMatch_noUserInToken_returnsFalse() {
        when(tokenBlacklistMapper.selectCount(any())).thenReturn(0L);
        when(jwtUtil.parseTokenIfValid("unknown-token")).thenReturn(null);

        assertThat(service.isBlacklisted("unknown-token")).isFalse();
    }

    @Test
    void isBlacklisted_userLevelBlacklist_returnsTrue() {
        // First call: direct token check = 0
        // Then for each TokenType, check user-level key
        when(tokenBlacklistMapper.selectCount(any())).thenReturn(0L).thenReturn(1L);
        Claims claims = new DefaultClaims();
        claims.setSubject("42");
        when(jwtUtil.parseTokenIfValid("user-token")).thenReturn(claims);

        assertThat(service.isBlacklisted("user-token")).isTrue();
    }

    @Test
    void isBlacklisted_noBlacklistAtAll_returnsFalse() {
        when(tokenBlacklistMapper.selectCount(any())).thenReturn(0L);
        Claims claims = new DefaultClaims();
        claims.setSubject("42");
        when(jwtUtil.parseTokenIfValid("clean-token")).thenReturn(claims);

        assertThat(service.isBlacklisted("clean-token")).isFalse();
    }

    @Test
    void removeFromBlacklist_deletesRecord() {
        when(tokenBlacklistMapper.delete(any())).thenReturn(1);

        service.removeFromBlacklist("some-token");

        verify(tokenBlacklistMapper).delete(any());
    }

    @Test
    void blacklistAllUserTokens_insertsUserLevelRecord() {
        when(jwtUtil.getExpirationSeconds()).thenReturn(86400L);
        when(tokenBlacklistMapper.insert(any(TokenBlacklist.class))).thenReturn(1);

        service.blacklistAllUserTokens(TokenType.User, 42L, "admin kick");

        verify(tokenBlacklistMapper).insert(any(TokenBlacklist.class));
    }
}

package online.longlian.app.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceImplTest {
    private final TokenRevocationStore store = mock(TokenRevocationStore.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private final TokenBlacklistServiceImpl service = new TokenBlacklistServiceImpl(store, jwt, clock);

    /** Expired tokens require no persistence. */
    @Test
    void shouldSkipExpiredToken() {
        service.addToBlacklist("token", TokenType.User, 1L, "logout", 0);
        service.addToBlacklist("token", TokenType.User, 1L, "logout", -1);
        verifyNoInteractions(store);
    }

    /** Persisted revocations contain a digest, never the original JWT. */
    @Test
    void shouldStoreOnlyTokenDigest() {
        service.addToBlacklist("private.jwt.token", TokenType.User, 1L, "logout", 60);
        var captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(TokenRevocationStore.digest("private.jwt.token"))
                .doesNotContain("private.jwt.token");
    }

    /** Invalid tokens do not query the store. */
    @Test
    void shouldSkipLookupForInvalidToken() {
        assertThat(service.isBlacklisted("invalid")).isFalse();
        verifyNoInteractions(store);
    }

    /** Missing identity type is an invalid credential rather than an infrastructure failure. */
    @Test
    void shouldRejectMissingTokenType() {
        when(jwt.parseTokenIfValid("untyped")).thenReturn(Jwts.claims().setSubject("1"));
        assertThatThrownBy(() -> service.isBlacklisted("untyped")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }

    /** A direct revocation is applied only while it is active. */
    @Test
    void shouldRespectDirectRevocationExpiry() {
        claims("token", "user", clock.millis());
        when(store.entries(TokenType.User, 1L)).thenReturn(Map.of(TokenRevocationStore.digest("token"), clock.millis() + 1));
        assertThat(service.isBlacklisted("token")).isTrue();
        when(store.entries(TokenType.User, 1L)).thenReturn(Map.of(TokenRevocationStore.digest("token"), clock.millis()));
        assertThat(service.isBlacklisted("token")).isFalse();
    }

    /** Tokens issued after a global cutoff remain valid, including within the same second. */
    @Test
    void shouldOnlyRevokeTokensIssuedAtOrBeforeCutoff() {
        claims("old", "user", clock.millis());
        claims("new", "user", clock.millis() + 1);
        when(store.entries(TokenType.User, 1L)).thenReturn(Map.of("before:" + clock.millis(), clock.millis() + 60_000));
        assertThat(service.isBlacklisted("old")).isTrue();
        assertThat(service.isBlacklisted("new")).isFalse();
    }

    /** Identical numeric IDs in the two identity domains remain isolated. */
    @Test
    void shouldScopeRevocationToTokenType() {
        claims("admin", "admin", clock.millis());
        when(store.entries(TokenType.Admin, 1L)).thenReturn(Map.of());
        assertThat(service.isBlacklisted("admin")).isFalse();
        verify(store).entries(TokenType.Admin, 1L);
        verify(store, never()).entries(TokenType.User, 1L);
    }

    /** Legacy JWTs use their standard issued-at claim. */
    @Test
    void shouldSupportLegacyIssuedAt() {
        Claims claims = Jwts.claims().setSubject("1").setIssuedAt(new Date(clock.millis() - 1000));
        claims.put("type", "user");
        when(jwt.parseTokenIfValid("legacy")).thenReturn(claims);
        when(store.entries(TokenType.User, 1L)).thenReturn(Map.of("before:" + clock.millis(), clock.millis() + 60_000));
        assertThat(service.isBlacklisted("legacy")).isTrue();
    }

    /** Global revocation persists a millisecond cutoff instead of a blanket user ban. */
    @Test
    void shouldPersistGlobalCutoff() {
        when(jwt.getExpirationSeconds()).thenReturn(3600L);
        service.blacklistAllUserTokens(TokenType.User, 1L, "kick");
        var captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("1:user:1:before:" + clock.millis());
    }

    private void claims(String token, String type, long issuedAt) {
        Claims claims = Jwts.claims().setSubject("1");
        claims.put("type", type);
        claims.put("issuedAtMillis", issuedAt);
        when(jwt.parseTokenIfValid(token)).thenReturn(claims);
    }
}

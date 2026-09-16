package online.longlian.app.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.bo.common.TokenRevocationEntryBO;
import online.longlian.app.pojo.bo.common.TokenRevocationSnapshotBO;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.common.enumeration.TokenType;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class TokenBlacklistServiceImplTest {
    private final TokenRevocationStore store = mock(TokenRevocationStore.class);
    private final JwtUtil jwt = mock(JwtUtil.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private final TokenBlacklistServiceImpl service = new TokenBlacklistServiceImpl(store, jwt, clock);

    /** 已过期 token 不需要持久化。 */
    @Test
    void shouldSkipExpiredToken() {
        service.addToBlacklist("token", TokenType.User, 1L, "logout", 0);
        service.addToBlacklist("token", TokenType.User, 1L, "logout", -1);
        verifyNoInteractions(store);
    }

    /** 持久化的吊销记录只包含摘要，不能保存原始 JWT。 */
    @Test
    void shouldStoreOnlyTokenDigest() {
        service.addToBlacklist("private.jwt.token", TokenType.User, 1L, "logout", 60);
        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo(TokenRevocationStore.digest("private.jwt.token"))
                .doesNotContain("private.jwt.token");
    }

    /** 无效 token 不应查询存储。 */
    @Test
    void shouldSkipLookupForInvalidToken() {
        assertThat(service.isBlacklisted("invalid")).isFalse();
        verifyNoInteractions(store);
    }

    /** 缺少身份类型属于无效凭证，而不是基础设施故障。 */
    @Test
    void shouldRejectMissingTokenType() {
        when(jwt.parseTokenIfValid("untyped")).thenReturn(Jwts.claims().setSubject("1"));
        assertThatThrownBy(() -> service.isBlacklisted("untyped")).isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(store);
    }

    /** 单 token 吊销只在有效期内生效。 */
    @Test
    void shouldRespectDirectRevocationExpiry() {
        claims("token", "user", clock.millis());
        when(store.entries(TokenType.User, 1L)).thenReturn(snapshot(TokenRevocationStore.digest("token"), clock.millis() + 1));
        assertThat(service.isBlacklisted("token")).isTrue();
        when(store.entries(TokenType.User, 1L)).thenReturn(snapshot(TokenRevocationStore.digest("token"), clock.millis()));
        assertThat(service.isBlacklisted("token")).isFalse();
    }

    /** 全量吊销截止时间之后签发的 token 仍然有效，同一秒内也一样。 */
    @Test
    void shouldOnlyRevokeTokensIssuedAtOrBeforeCutoff() {
        claims("old", "user", clock.millis());
        claims("new", "user", clock.millis() + 1);
        when(store.entries(TokenType.User, 1L)).thenReturn(snapshot("before:" + clock.millis(), clock.millis() + 60_000));
        assertThat(service.isBlacklisted("old")).isTrue();
        assertThat(service.isBlacklisted("new")).isFalse();
    }

    /** 用户和管理员身份域中的相同数字 ID 仍然相互隔离。 */
    @Test
    void shouldScopeRevocationToTokenType() {
        claims("admin", "admin", clock.millis());
        when(store.entries(TokenType.Admin, 1L)).thenReturn(TokenRevocationSnapshotBO.builder().build());
        assertThat(service.isBlacklisted("admin")).isFalse();
        verify(store).entries(TokenType.Admin, 1L);
        verify(store, never()).entries(TokenType.User, 1L);
    }

    /** 历史 JWT 使用标准的签发时间字段。 */
    @Test
    void shouldSupportLegacyIssuedAt() {
        Claims claims = Jwts.claims().setSubject("1").setIssuedAt(new Date(clock.millis() - 1000));
        claims.put("type", "user");
        when(jwt.parseTokenIfValid("legacy")).thenReturn(claims);
        when(store.entries(TokenType.User, 1L)).thenReturn(snapshot("before:" + clock.millis(), clock.millis() + 60_000));
        assertThat(service.isBlacklisted("legacy")).isTrue();
    }

    /** 全量吊销持久化毫秒级截止时间，而不是永久禁止用户。 */
    @Test
    void shouldPersistGlobalCutoff() {
        when(jwt.getExpirationSeconds()).thenReturn(3600L);
        service.blacklistAllUserTokens(TokenType.User, 1L, "kick");
        ArgumentCaptor<TokenBlacklist> captor = ArgumentCaptor.forClass(TokenBlacklist.class);
        verify(store).save(captor.capture());
        assertThat(captor.getValue().getToken()).isEqualTo("1:user:1:before:" + clock.millis());
    }

    @Test
    void shouldRemoveValidTokenUsingItsIdentity() {
        Claims claims = Jwts.claims().setSubject("1");
        claims.put("type", "user");
        when(jwt.parseToken("token")).thenReturn(claims);

        service.removeFromBlacklist("token");

        verify(store).remove(TokenType.User, 1L, "token");
    }

    @Test
    void shouldRemoveExpiredTokenUsingItsEmbeddedClaims() {
        Claims claims = Jwts.claims().setSubject("1");
        claims.put("type", "user");
        when(jwt.parseToken("expired")).thenThrow(new ExpiredJwtException(null, claims, "expired"));

        service.removeFromBlacklist("expired");

        verify(store).remove(TokenType.User, 1L, "expired");
    }

    private void claims(String token, String type, long issuedAt) {
        Claims claims = Jwts.claims().setSubject("1");
        claims.put("type", type);
        claims.put("issuedAtMillis", issuedAt);
        when(jwt.parseTokenIfValid(token)).thenReturn(claims);
    }

    private TokenRevocationSnapshotBO snapshot(String key, long expiredAtMillis) {
        return TokenRevocationSnapshotBO.builder()
                .entries(List.of(TokenRevocationEntryBO.builder()
                        .key(key).expiredAtMillis(expiredAtMillis).build()))
                .build();
    }
}

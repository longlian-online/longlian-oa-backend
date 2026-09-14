package online.longlian.app.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.common.enumeration.TokenType;
import online.longlian.common.service.DistributedLockService;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TokenRevocationStoreTest {
    private final TokenBlacklistMapper mapper = mock(TokenBlacklistMapper.class);
    private final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    @SuppressWarnings("unchecked")
    private final ValueOperations<String, String> values = mock(ValueOperations.class);
    private final DistributedLockService locks = mock(DistributedLockService.class);
    private final DistributedLockService.Lock lock = mock(DistributedLockService.Lock.class);
    private final PlatformTransactionManager transactions = mock(PlatformTransactionManager.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private final TokenRevocationStore store = new TokenRevocationStore(mapper, redis, new ObjectMapper(), locks, transactions, clock);
    private final String key = TokenRevocationStore.cacheKey(TokenType.User, 1L);

    @BeforeAll
    static void initializeTableMetadata() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), TokenBlacklist.class);
    }

    @BeforeEach
    void setUp() {
        when(locks.tryAcquire(key, 2, TimeUnit.SECONDS)).thenReturn(lock);
        when(redis.opsForValue()).thenReturn(values);
        when(transactions.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
    }

    @AfterEach
    void clearTransactions() {
        TransactionSynchronizationManager.clear();
    }

    /** An existing transaction keeps the identity lock through commit or rollback without another connection. */
    @Test
    void shouldHoldLockUntilOuterTransactionCompletes() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        store.save(row("sha256:token"));
        verify(lock, never()).close();
        verify(transactions, never()).getTransaction(any());
        TransactionSynchronizationManager.getSynchronizations().forEach(sync ->
                sync.afterCompletion(TransactionSynchronization.STATUS_COMMITTED));
        verify(lock).close();
    }

    /** Warm empty snapshots avoid database queries as well as positive snapshots. */
    @Test
    void shouldReadWarmSnapshotWithoutDatabase() {
        when(values.get(key)).thenReturn("{}");
        assertThat(store.entries(TokenType.User, 1L)).isEmpty();
        verifyNoInteractions(mapper);
        verify(lock).close();
    }

    @Test
    void shouldFallBackWhenCachedSnapshotCannotBeDecoded() throws Exception {
        when(values.get(key)).thenReturn("not-json");
        when(mapper.selectList(any())).thenReturn(List.of(row("legacy.jwt.token")));

        assertThat(store.entries(TokenType.User, 1L))
                .containsKey(TokenRevocationStore.digest("legacy.jwt.token"));
        verify(mapper).selectList(any());
        verify(lock).close();
    }

    @Test
    void shouldReturnDatabaseEntriesWhenCacheWriteFails() {
        when(mapper.selectList(any())).thenReturn(List.of(row("legacy.jwt.token")));
        doThrow(new IllegalStateException("offline")).when(values)
                .set(anyString(), anyString(), any(Duration.class));

        assertThat(store.entries(TokenType.User, 1L))
                .containsKey(TokenRevocationStore.digest("legacy.jwt.token"));
        verify(lock).close();
    }

    /** Cold snapshots hash legacy tokens and have a bounded TTL. */
    @Test
    void shouldLoadLegacyRowsAndCacheOnlyDigests() {
        when(mapper.selectList(any())).thenReturn(List.of(row("legacy.jwt.token")));
        assertThat(store.entries(TokenType.User, 1L))
                .containsEntry(TokenRevocationStore.digest("legacy.jwt.token"), clock.millis() + 60_000);
        verify(mapper, times(1)).selectList(any());
        verify(values).set(eq(key), argThat(value -> !value.contains("legacy.jwt.token")), eq(Duration.ofSeconds(60)));
    }

    /** Historic blanket revocations are interpreted as a cutoff at creation time. */
    @Test
    void shouldReadLegacyGlobalRevocation() {
        when(mapper.selectList(any())).thenReturn(List.of(row("1:user:1:all")));
        assertThat(store.entries(TokenType.User, 1L)).containsKey("before:" + clock.millis());
    }

    /** Redis outages fall back to persisted revocations. */
    @Test
    void shouldFallBackToDatabaseWhenRedisIsUnavailable() {
        when(locks.tryAcquire(key, 2, TimeUnit.SECONDS)).thenThrow(new IllegalStateException("offline"));
        when(mapper.selectList(any())).thenReturn(List.of(row("legacy.jwt.token")));
        assertThat(store.entries(TokenType.User, 1L)).containsKey(TokenRevocationStore.digest("legacy.jwt.token"));
        verifyNoInteractions(values);
    }

    /** Failure of both storage paths never produces an empty allow-list snapshot. */
    @Test
    void shouldPropagateDatabaseFailureOnFallback() {
        when(locks.tryAcquire(key, 2, TimeUnit.SECONDS)).thenReturn(null);
        when(mapper.selectList(any())).thenThrow(new IllegalStateException("database unavailable"));
        assertThatThrownBy(() -> store.entries(TokenType.User, 1L)).isInstanceOf(IllegalStateException.class);
    }

    /** Cache invalidation and DB commit both complete before releasing the identity lock. */
    @Test
    void shouldCommitRevocationBeforeUnlocking() {
        TokenBlacklist entry = row(TokenRevocationStore.digest("token"));
        store.save(entry);
        var order = inOrder(redis, mapper, transactions, lock);
        order.verify(redis).delete(key);
        order.verify(transactions).getTransaction(any());
        order.verify(mapper).selectOne(any());
        order.verify(mapper).insert(entry);
        order.verify(transactions).commit(any());
        order.verify(lock).close();
    }

    /** If cached grants cannot be invalidated, the operation must fail rather than acknowledge logout. */
    @Test
    void shouldRejectWriteWhenCacheInvalidationFails() {
        when(redis.delete(key)).thenThrow(new IllegalStateException("offline"));
        assertThatThrownBy(() -> store.save(row("sha256:token"))).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
        verify(transactions, never()).getTransaction(any());
        verify(lock).close();
    }

    /** Repeated revocation is idempotent and cannot shorten an existing revocation. */
    @Test
    void shouldPreserveLongerExistingRevocation() {
        TokenBlacklist existing = row("sha256:token");
        existing.setId(7L);
        existing.setExpiredAt(LocalDateTime.now(clock).plusHours(1));
        when(mapper.selectOne(any())).thenReturn(existing);
        TokenBlacklist incoming = row("sha256:token");
        store.save(incoming);
        assertThat(incoming.getId()).isEqualTo(7L);
        assertThat(incoming.getExpiredAt()).isEqualTo(existing.getExpiredAt());
        verify(mapper).updateById(incoming);
        verify(mapper, never()).insert(any(TokenBlacklist.class));
    }

    @Test
    void shouldRemoveDigestAndLegacyTokenBeforeUnlocking() {
        store.remove(TokenType.User, 1L, "legacy.jwt.token");

        verify(mapper).delete(any());
        verify(lock).close();
    }

    @Test
    void shouldRejectMutationWhenIdentityLockIsBusy() {
        when(locks.tryAcquire(key, 2, TimeUnit.SECONDS)).thenReturn(null);

        assertThatThrownBy(() -> store.save(row("sha256:busy")))
                .isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(mapper);
        verifyNoInteractions(redis);
    }

    private TokenBlacklist row(String token) {
        return TokenBlacklist.builder().token(token).tokenType(TokenType.User).userId(1L)
                .createdAt(LocalDateTime.now(clock)).expiredAt(LocalDateTime.now(clock).plusSeconds(60)).build();
    }
}

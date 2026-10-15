package online.longlian.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.common.service.DistributedLockService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 用户刷新凭证存在 Redis，只保存哈希。一次登录对应一条会话；轮换后的上一代凭证在宽限期内
 * 重复提交时返回同一次换发结果，超过宽限或密码变更后整段会话作废。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshSessionStore {

    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DistributedLockService locks;

    public CreatedRefresh create(long userId, long createdAt, Duration ttl, Supplier<String> refreshTokens) {
        try (DistributedLockService.Lock ignored = acquire(userId)) {
            String refreshToken = refreshTokens.get();
            String sessionId = UUID.randomUUID().toString();
            RefreshSessionRecord record = new RefreshSessionRecord();
            record.setUserId(userId);
            record.setCreatedAt(createdAt);
            record.setCurrentHash(TokenRevocationStore.digest(refreshToken));
            persist(sessionId, record, null, ttl);
            return new CreatedRefresh(sessionId, refreshToken);
        }
    }

    public RefreshStatus exchange(String presentedRefresh, long now, long graceMillis, Duration ttl, RefreshIssuer issuer) {
        String presentedHash = TokenRevocationStore.digest(presentedRefresh);
        String sessionId = redis.opsForValue().get(RedisConstants.REFRESH_HASH + presentedHash);
        RefreshSessionRecord preview = sessionId == null ? null : read(sessionId);
        if (preview == null) {
            return RefreshStatus.rejected();
        }
        try (DistributedLockService.Lock ignored = acquire(preview.getUserId())) {
            RefreshSessionRecord session = read(sessionId);
            if (session == null) {
                return RefreshStatus.rejected();
            }
            Long revokedAt = revokedAt(session.getUserId());
            RefreshDecision decision = decide(session, presentedHash, now, revokedAt);
            if (decision == RefreshDecision.REJECT) {
                return RefreshStatus.rejected();
            }
            if (decision == RefreshDecision.REVOKE) {
                deleteRecord(sessionId, session);
                return RefreshStatus.revoked();
            }
            if (decision == RefreshDecision.REPLAY) {
                persist(sessionId, session, null, ttl);
                return RefreshStatus.exchanged(new RefreshExchange(session.getSuccessorAccess(), session.getSuccessorRefresh()));
            }
            RefreshExchange issued;
            try {
                issued = issuer.issue(session.getUserId(), sessionId);
            } catch (AccountUnavailable unavailable) {
                deleteAllUnlocked(session.getUserId());
                return RefreshStatus.revoked();
            }
            String retiredHash = session.getPreviousHash();
            session.setPreviousHash(session.getCurrentHash());
            session.setPreviousValidUntil(now + graceMillis);
            session.setSuccessorAccess(issued.accessToken());
            session.setSuccessorRefresh(issued.refreshToken());
            session.setCurrentHash(TokenRevocationStore.digest(issued.refreshToken()));
            persist(sessionId, session, retiredHash, ttl);
            return RefreshStatus.exchanged(issued);
        }
    }

    public void deleteOne(String sessionId, long userId) {
        try (DistributedLockService.Lock ignored = acquire(userId)) {
            RefreshSessionRecord record = read(sessionId);
            if (record == null || record.getUserId() != userId) {
                return;
            }
            deleteRecord(sessionId, record);
        }
    }

    /** 密码变更先挡住旧会话，再让访问凭证黑名单覆盖已经发出的访问凭证。 */
    public void revoke(long userId, long revokedAt, Duration ttl) {
        try (DistributedLockService.Lock ignored = acquire(userId)) {
            redis.opsForValue().set(RedisConstants.REFRESH_REVOKED + userId, Long.toString(revokedAt), ttl);
            deleteAllUnlocked(userId);
        }
    }

    static RefreshDecision decide(RefreshSessionRecord session, String presentedHash, long now, Long revokedAt) {
        if (session == null || presentedHash == null) {
            return RefreshDecision.REJECT;
        }
        if (revokedAt != null && session.getCreatedAt() <= revokedAt) {
            return RefreshDecision.REVOKE;
        }
        if (presentedHash.equals(session.getCurrentHash())) {
            return RefreshDecision.ROTATE;
        }
        if (presentedHash.equals(session.getPreviousHash())) {
            boolean replayable = session.getPreviousValidUntil() != null
                    && now <= session.getPreviousValidUntil()
                    && session.getSuccessorAccess() != null
                    && session.getSuccessorRefresh() != null;
            return replayable ? RefreshDecision.REPLAY : RefreshDecision.REVOKE;
        }
        return RefreshDecision.REJECT;
    }

    private void persist(String sessionId, RefreshSessionRecord record, String retiredHash, Duration ttl) {
        try {
            redis.opsForValue().set(RedisConstants.REFRESH_SESSION + sessionId,
                    objectMapper.writeValueAsString(record), ttl);
        } catch (Exception e) {
            throw new IllegalStateException("刷新会话写入失败", e);
        }
        redis.opsForValue().set(RedisConstants.REFRESH_HASH + record.getCurrentHash(), sessionId, ttl);
        if (record.getPreviousHash() != null) {
            redis.opsForValue().set(RedisConstants.REFRESH_HASH + record.getPreviousHash(), sessionId, ttl);
        }
        if (retiredHash != null && !retiredHash.equals(record.getCurrentHash())
                && !retiredHash.equals(record.getPreviousHash())) {
            redis.delete(RedisConstants.REFRESH_HASH + retiredHash);
        }
        redis.opsForSet().add(RedisConstants.REFRESH_USER + record.getUserId(), sessionId);
        redis.expire(RedisConstants.REFRESH_USER + record.getUserId(), ttl);
    }

    private void deleteAllUnlocked(long userId) {
        Set<String> sessionIds = redis.opsForSet().members(RedisConstants.REFRESH_USER + userId);
        if (sessionIds != null) {
            for (String sessionId : sessionIds) {
                RefreshSessionRecord record = read(sessionId);
                if (record == null) {
                    redis.delete(RedisConstants.REFRESH_SESSION + sessionId);
                } else {
                    deleteRecord(sessionId, record);
                }
            }
        }
        redis.delete(RedisConstants.REFRESH_USER + userId);
    }

    private void deleteRecord(String sessionId, RefreshSessionRecord record) {
        redis.delete(RedisConstants.REFRESH_SESSION + sessionId);
        if (record.getCurrentHash() != null) {
            redis.delete(RedisConstants.REFRESH_HASH + record.getCurrentHash());
        }
        if (record.getPreviousHash() != null) {
            redis.delete(RedisConstants.REFRESH_HASH + record.getPreviousHash());
        }
        redis.opsForSet().remove(RedisConstants.REFRESH_USER + record.getUserId(), sessionId);
    }

    private RefreshSessionRecord read(String sessionId) {
        String json = redis.opsForValue().get(RedisConstants.REFRESH_SESSION + sessionId);
        if (json == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, RefreshSessionRecord.class);
        } catch (Exception e) {
            log.warn("刷新会话无法读取，sessionId={}", sessionId);
            return null;
        }
    }

    private Long revokedAt(long userId) {
        String value = redis.opsForValue().get(RedisConstants.REFRESH_REVOKED + userId);
        if (value == null) {
            return null;
        }
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private DistributedLockService.Lock acquire(long userId) {
        DistributedLockService.Lock lock = locks.tryAcquire(
                "auth:refresh-user:" + userId, 2, TimeUnit.SECONDS);
        if (lock == null) {
            throw new IllegalStateException("刷新会话更新繁忙，请重试");
        }
        return lock;
    }

    public enum RefreshDecision {
        ROTATE, REPLAY, REVOKE, REJECT
    }

    @FunctionalInterface
    public interface RefreshIssuer {
        RefreshExchange issue(long userId, String sessionId);
    }

    /** 用户已禁用或不存在。调用方在持锁期间抛出，存储层据此清掉该用户的刷新会话。 */
    public static class AccountUnavailable extends RuntimeException {
    }

    public record CreatedRefresh(String sessionId, String refreshToken) {
    }

    public record RefreshExchange(String accessToken, String refreshToken) {
    }

    public record RefreshStatus(Kind kind, RefreshExchange exchange) {
        public static RefreshStatus exchanged(RefreshExchange exchange) {
            return new RefreshStatus(Kind.EXCHANGED, exchange);
        }

        public static RefreshStatus rejected() {
            return new RefreshStatus(Kind.REJECTED, null);
        }

        public static RefreshStatus revoked() {
            return new RefreshStatus(Kind.REVOKED, null);
        }

        public enum Kind {
            EXCHANGED, REJECTED, REVOKED
        }
    }
}

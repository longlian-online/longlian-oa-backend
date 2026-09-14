package online.longlian.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.common.enumeration.TokenType;
import online.longlian.common.service.DistributedLockService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class TokenRevocationStore {
    private final TokenBlacklistMapper mapper;
    private final StringRedisTemplate redis;
    private final ObjectMapper objectMapper;
    private final DistributedLockService locks;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    public Map<String, Long> entries(TokenType type, long userId) {
        DistributedLockService.Lock lock;
        try {
            lock = locks.tryAcquire(cacheKey(type, userId), 2, TimeUnit.SECONDS);
        } catch (RuntimeException e) {
            log.warn("撤销缓存锁不可用，使用数据库校验 | type={}", e.getClass().getSimpleName());
            return load(type, userId);
        }
        if (lock == null) {
            return load(type, userId);
        }
        try (lock) {
            try {
                String cached = redis.opsForValue().get(cacheKey(type, userId));
                if (cached != null) {
                    return objectMapper.readValue(cached, new TypeReference<Map<String, Long>>() { });
                }
            } catch (Exception e) {
                log.warn("撤销缓存读取失败，使用数据库校验 | type={}", e.getClass().getSimpleName());
                return load(type, userId);
            }
            Map<String, Long> entries = load(type, userId);
            long latestExpiry = entries.values().stream().mapToLong(Long::longValue).max().orElse(clock.millis() + 60_000);
            long ttl = Math.max(1, Math.min(60_000, latestExpiry - clock.millis()));
            try {
                redis.opsForValue().set(cacheKey(type, userId), objectMapper.writeValueAsString(entries), Duration.ofMillis(ttl));
            } catch (Exception e) {
                log.warn("撤销缓存写入失败 | type={}", e.getClass().getSimpleName());
            }
            return entries;
        }
    }

    public void save(TokenBlacklist entry) {
        mutate(entry.getTokenType(), entry.getUserId(), () -> {
            TokenBlacklist existing = mapper.selectOne(new LambdaQueryWrapper<TokenBlacklist>()
                    .eq(TokenBlacklist::getToken, entry.getToken()));
            if (existing == null) {
                mapper.insert(entry);
            } else {
                entry.setId(existing.getId());
                if (existing.getExpiredAt().isAfter(entry.getExpiredAt())) {
                    entry.setExpiredAt(existing.getExpiredAt());
                }
                mapper.updateById(entry);
            }
        });
    }

    public void remove(TokenType type, long userId, String token) {
        mutate(type, userId, () -> mapper.delete(new LambdaQueryWrapper<TokenBlacklist>()
                .eq(TokenBlacklist::getTokenType, type).eq(TokenBlacklist::getUserId, userId)
                .in(TokenBlacklist::getToken, digest(token), token)));
    }

    private void mutate(TokenType type, long userId, Runnable mutation) {
        var lock = locks.tryAcquire(cacheKey(type, userId), 2, TimeUnit.SECONDS);
        if (lock == null) {
            throw new IllegalStateException("撤销状态更新繁忙，请重试");
        }
        boolean releaseOnCompletion = false;
        try {
            // 在提交前失效缓存，并由同一把锁排除读取和填充操作。
            // Redis 失效失败时不能确认吊销成功，避免继续使用过期的授权缓存。
            redis.delete(cacheKey(type, userId));
            if (TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        lock.close();
                    }
                });
                releaseOnCompletion = true;
                mutation.run();
            } else {
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> mutation.run());
            }
        } finally {
            if (!releaseOnCompletion) {
                lock.close();
            }
        }
    }

    private Map<String, Long> load(TokenType type, long userId) {
        var rows = mapper.selectList(new LambdaQueryWrapper<TokenBlacklist>()
                .eq(TokenBlacklist::getTokenType, type).eq(TokenBlacklist::getUserId, userId)
                .gt(TokenBlacklist::getExpiredAt, LocalDateTime.now(clock)));
        Map<String, Long> entries = new HashMap<>();
        String identity = type.getCode() + ":user:" + userId + ":";
        for (TokenBlacklist row : rows) {
            String key;
            if (row.getToken().equals(identity + "all")) {
                key = "before:" + row.getCreatedAt().atZone(clock.getZone()).toInstant().toEpochMilli();
            } else if (row.getToken().startsWith(identity + "before:")) {
                key = row.getToken().substring(identity.length());
            } else {
                key = row.getToken().startsWith("sha256:") ? row.getToken() : digest(row.getToken());
            }
            entries.merge(key, row.getExpiredAt().atZone(clock.getZone()).toInstant().toEpochMilli(), Math::max);
        }
        return entries;
    }

    static String cacheKey(TokenType type, long userId) {
        return "auth:revocations:v2:" + type.getCode() + ":" + userId;
    }

    public static String digest(String token) {
        try {
            return "sha256:" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8)));
        // JDK 保证 SHA-256 算法存在，该异常分支无法通过测试触发。
        } catch (NoSuchAlgorithmException e) { // skipcq: TCV-001
            throw new IllegalStateException("SHA-256 unavailable", e); // skipcq: TCV-001
        }
    }
}

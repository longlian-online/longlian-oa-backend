package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.properties.OpenApiProperties;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RedissonClient;
import org.redisson.api.RateType;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * 开放接口按密钥限流。Redis 故障按 standard.md「Redis 仅加速」降级放行并告警，
 * 不因限流组件故障拒绝全部外部流量。
 * <p>
 * 限流参数只在键首次创建时写入 Redis；调整配置后需删除 open:rate:* 键生效。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OpenApiRateLimitService {

    private static final String RATE_LIMIT_KEY_PREFIX = "open:rate:";

    private final RedissonClient redissonClient;
    private final OpenApiProperties openApiProperties;

    public boolean tryAcquire(long keyId) {
        try {
            RRateLimiter limiter = redissonClient.getRateLimiter(RATE_LIMIT_KEY_PREFIX + keyId);
            limiter.trySetRate(RateType.OVERALL, openApiProperties.getRateLimitPerMinute(), Duration.ofMinutes(1));
            return limiter.tryAcquire(1);
        } catch (RuntimeException e) {
            log.warn("开放接口限流不可用，降级放行 | keyId={}", keyId, e);
            return true;
        }
    }
}

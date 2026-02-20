package online.longlian.app.common.util;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.RedisConstants;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedisBlacklistUtil {

    private final RedisTemplate<String, Object> redisTemplate;
    /**
     * 将令牌加入黑名单
     */
    public void addToBlacklist(String token, long expireSeconds) {
        if (expireSeconds > 0) {
            redisTemplate.opsForValue().set(
                    RedisConstants.BLACKLIST + token,
                    "1",  //用于标记
                    expireSeconds,
                    TimeUnit.SECONDS
            );
        }
    }
    /**
     * 检查令牌是否在黑名单中
     */
    public boolean isInBlacklist(String token) {
        return redisTemplate.hasKey(RedisConstants.BLACKLIST + token);
    }
    /**
     * 移除黑名单中的令牌
     */
    public void removeFromBlacklist(String token) {
        redisTemplate.delete(RedisConstants.BLACKLIST + token);
    }
}
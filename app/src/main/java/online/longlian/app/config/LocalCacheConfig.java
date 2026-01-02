package online.longlian.app.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

// 本地缓存：Caffeine (内存)
@Configuration
@ConditionalOnProperty(value = "longlian.cache.type", havingValue = "local", matchIfMissing = true)
@EnableAutoConfiguration(exclude = {
        RedisAutoConfiguration.class,
        RedisRepositoriesAutoConfiguration.class
})
@Slf4j
public class LocalCacheConfig {

    @Bean
    @Primary
    public CacheManager cacheManager() {
        log.info("开始初始化本地Caffeine缓存，配置：过期时间10分钟，最大缓存容量1000条");
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)  // 10分钟过期
                .maximumSize(1000));                     // 最大1000条
        return cacheManager;
    }
}

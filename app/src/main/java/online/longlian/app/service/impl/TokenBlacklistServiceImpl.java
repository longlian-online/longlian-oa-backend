package online.longlian.app.service.impl;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.util.RedisBlacklistUtil;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.generator.enumeration.TokenType;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@AllArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisBlacklistUtil redisBlacklistUtil;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public void addToBlacklist(String token, TokenType tokenType, Long userId, String reason, long expireSeconds) {
        redisBlacklistUtil.addToBlacklist(token, expireSeconds);
        log.info("Token已加入黑名单, tokenType={}, userId={}, reason={}", tokenType, userId, reason);
    }

    @Override
    public boolean isBlacklisted(String token) {
        return redisBlacklistUtil.isInBlacklist(token);
    }

    @Override
    public void blacklistAllUserTokens(TokenType tokenType, Long userId, String reason) {
        log.info("将用户所有Token加入黑名单, tokenType={}, userId={}, reason={}", tokenType, userId, reason);
        String key = RedisConstants.BLACKLIST + "user:" + tokenType.getCode() + ":" + userId;
        redisTemplate.opsForValue().set(key, "1", 24, TimeUnit.HOURS);
    }
}

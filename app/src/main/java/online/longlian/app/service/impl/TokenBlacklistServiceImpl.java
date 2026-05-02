package online.longlian.app.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.mapper.TokenBlacklistMapper;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.generator.enumeration.TokenType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@AllArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final TokenBlacklistMapper tokenBlacklistMapper;
    private final JwtUtil jwtUtil;

    @Override
    public void addToBlacklist(String token, TokenType tokenType, Long userId, String reason, long expireSeconds) {
        if (expireSeconds <= 0) {
            log.warn("Token已过期，无需加入黑名单, tokenType={}, userId={}, reason={}", tokenType, userId, reason);
            return;
        }

        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(expireSeconds);

        TokenBlacklist entity = TokenBlacklist.builder()
                .token(token)
                .tokenType(tokenType)
                .userId(userId)
                .reason(reason)
                .expiredAt(expiredAt)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tokenBlacklistMapper.insert(entity);
        log.info("Token已加入黑名单, tokenType={}, userId={}, reason={}", tokenType, userId, reason);
    }

    @Override
    public boolean isBlacklisted(String token) {
        LocalDateTime now = LocalDateTime.now();
        // 1. 检查该 token 是否单独被加入黑名单
        Long count = tokenBlacklistMapper.selectCount(
                new LambdaQueryWrapper<TokenBlacklist>()
                        .eq(TokenBlacklist::getToken, token)
                        .gt(TokenBlacklist::getExpiredAt, now)
        );
        if (count != null && count > 0) {
            return true;
        }

        // 2. 检查该 token 所属用户是否被全局踢掉（blacklistAllUserTokens）
        //    从 token 中解析 userId
        Long userId = jwtUtil.parseTokenIfValid(token) != null
                ? Long.parseLong(jwtUtil.parseTokenIfValid(token).getSubject())
                : null;
        if (userId == null) {
            return false;
        }

        // 查询是否有该用户的全局黑名单记录（token 格式为 "{tokenType.code}:user:{userId}:all"）
        for (TokenType tokenType : TokenType.values()) {
            String userLevelKey = tokenType.getCode() + ":user:" + userId + ":all";
            Long userLevelCount = tokenBlacklistMapper.selectCount(
                    new LambdaQueryWrapper<TokenBlacklist>()
                            .eq(TokenBlacklist::getToken, userLevelKey)
                            .gt(TokenBlacklist::getExpiredAt, now)
            );
            if (userLevelCount != null && userLevelCount > 0) {
                return true;
            }
        }

        return false;
    }

    @Override
    public void removeFromBlacklist(String token) {
        tokenBlacklistMapper.delete(
                new LambdaQueryWrapper<TokenBlacklist>()
                        .eq(TokenBlacklist::getToken, token)
        );
    }

    @Override
    public void blacklistAllUserTokens(TokenType tokenType, Long userId, String reason) {
        log.info("将用户所有Token加入黑名单, tokenType={}, userId={}, reason={}", tokenType, userId, reason);
        // 对于"踢掉所有token"的场景，无法获取每个token的具体过期时间，
        // 使用JWT默认过期时间作为黑名单记录的过期时间
        long expireSeconds = jwtUtil.getExpirationSeconds();
        LocalDateTime expiredAt = LocalDateTime.now().plusSeconds(expireSeconds);

        TokenBlacklist entity = TokenBlacklist.builder()
                .token(tokenType.getCode() + ":user:" + userId + ":all")
                .tokenType(tokenType)
                .userId(userId)
                .reason(reason)
                .expiredAt(expiredAt)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        tokenBlacklistMapper.insert(entity);
    }
}

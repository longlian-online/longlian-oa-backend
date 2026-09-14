package online.longlian.app.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.pojo.entity.TokenBlacklist;
import online.longlian.app.service.TokenBlacklistService;
import online.longlian.common.enumeration.TokenType;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {
    private final TokenRevocationStore store;
    private final JwtUtil jwtUtil;
    private final Clock clock;

    @Override
    public void addToBlacklist(String token, TokenType tokenType, Long userId, String reason, long expireSeconds) {
        if (expireSeconds <= 0) {
            return;
        }
        save(TokenRevocationStore.digest(token), tokenType, userId, reason, expireSeconds);
    }

    @Override
    public boolean isBlacklisted(String token) {
        Claims claims = jwtUtil.parseTokenIfValid(token);
        if (claims == null) {
            return false;
        }
        TokenType type = tokenType(claims);
        long userId = Long.parseLong(claims.getSubject());
        Long issuedAtMillis = claims.get("issuedAtMillis", Long.class);
        long issuedAt = issuedAtMillis != null ? issuedAtMillis
                : claims.getIssuedAt() != null ? claims.getIssuedAt().getTime() : Long.MIN_VALUE;
        String digest = TokenRevocationStore.digest(token);
        long now = clock.millis();
        return store.entries(type, userId).entrySet().stream().anyMatch(entry ->
                entry.getValue() > now && (entry.getKey().equals(digest)
                        || entry.getKey().startsWith("before:")
                        && issuedAt <= Long.parseLong(entry.getKey().substring("before:".length()))));
    }

    @Override
    public void removeFromBlacklist(String token) {
        Claims claims;
        try {
            claims = jwtUtil.parseToken(token);
        } catch (ExpiredJwtException e) {
            claims = e.getClaims();
        }
        store.remove(tokenType(claims), Long.parseLong(claims.getSubject()), token);
    }

    @Override
    public void blacklistAllUserTokens(TokenType tokenType, Long userId, String reason) {
        String key = tokenType.getCode() + ":user:" + userId + ":before:" + clock.millis();
        save(key, tokenType, userId, reason, jwtUtil.getExpirationSeconds());
    }

    private void save(String key, TokenType type, Long userId, String reason, long expireSeconds) {
        LocalDateTime now = LocalDateTime.now(clock);
        store.save(TokenBlacklist.builder().token(key).tokenType(type).userId(userId).reason(reason)
                .createdAt(now).updatedAt(now).expiredAt(now.plusSeconds(expireSeconds)).build());
    }

    private TokenType tokenType(Claims claims) {
        return switch (claims.get("type", String.class)) {
            case "user" -> TokenType.User;
            case "admin" -> TokenType.Admin;
            default -> throw new IllegalArgumentException("未知凭证类型");
        };
    }
}

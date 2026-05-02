package online.longlian.app.service;

import online.longlian.generator.enumeration.TokenType;

public interface TokenBlacklistService {

    void addToBlacklist(String token, TokenType tokenType, Long userId, String reason, long expireSeconds);

    boolean isBlacklisted(String token);

    void removeFromBlacklist(String token);

    void blacklistAllUserTokens(TokenType tokenType, Long userId, String reason);
}

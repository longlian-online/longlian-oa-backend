package online.longlian.app.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.time.Clock;

@Component
@RequiredArgsConstructor
public class JwtUtil {
    private final Clock clock;

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Integer expiration; // 管理端访问凭证，秒

    @Value("${jwt.user-access-expiration:21600}")
    private Long userAccessExpiration = 21600L;

    @Value("${jwt.refresh-expiration:1209600}")
    private Long refreshExpiration = 1_209_600L;

    @Value("${jwt.refresh-grace-seconds:30}")
    private Long refreshGraceSeconds = 30L;

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId) {
        return generateToken(userId, null);
    }

    public String generateToken(Long id, String type) {
        return generateToken(id, type, null, null, expiration);
    }

    public String generateAccessToken(Long userId, String sessionId) {
        return generateToken(userId, "user", "access", sessionId, userAccessExpiration);
    }

    public String generateToken(Long id, String type, String use, String sessionId, long ttlSeconds) {
        long issuedAt = clock.millis();
        var builder = Jwts.builder()
                .setSubject(id.toString())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(issuedAt))
                .claim("issuedAtMillis", issuedAt)
                .setExpiration(new Date(issuedAt + ttlSeconds * 1000L));
        if (type != null && !type.isEmpty()) {
            builder.claim("type", type);
        }
        if (use != null && !use.isEmpty()) {
            builder.claim("use", use);
        }
        if (sessionId != null && !sessionId.isEmpty()) {
            builder.claim("sid", sessionId);
        }
        return builder.signWith(signingKey, SignatureAlgorithm.HS256).compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .setClock(() -> Date.from(clock.instant()))
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Claims parseTokenIfValid(String token) {
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            return null; // token 过期返回 null
        } catch (Exception e) {
            return null;
        }
    }

    /** 登出时访问凭证可能刚过期，仍要取出 sid 作废对应的刷新会话。签名无效则返回 null。 */
    public Claims parseAllowingExpiry(String token) {
        try {
            return parseToken(token);
        } catch (ExpiredJwtException e) {
            return e.getClaims();
        } catch (JwtException e) {
            return null;
        }
    }

    public long getRemainingTimeSeconds(String token) {
        try {
            Claims claims = parseToken(token);
            Date expireDate = claims.getExpiration();
            long remaining = (expireDate.getTime() - clock.millis() + 999) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            return 0;
        }
    }

    public long getExpirationSeconds() {
        return expiration == null ? 0 : expiration.longValue();
    }

    public long getUserAccessExpirationSeconds() {
        return userAccessExpiration == null ? 0 : userAccessExpiration;
    }

    public long getRefreshExpirationSeconds() {
        return refreshExpiration == null ? 0 : refreshExpiration;
    }

    public long getRefreshGraceSeconds() {
        return refreshGraceSeconds == null ? 0 : refreshGraceSeconds;
    }
}

package online.longlian.app.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
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
    private Integer expiration; // 秒

    private SecretKey signingKey;

    @PostConstruct
    public void init() {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId) {
        return generateToken(userId, null, null);
    }

    public String generateToken(Long id, String type) {
        return generateToken(id, type, null);
    }

    public String generateToken(Long id, String type, Integer authVersion) {
        long issuedAt = clock.millis();
        var builder = Jwts.builder()
                .setSubject(id.toString())
                .setId(UUID.randomUUID().toString())
                .setIssuedAt(new Date(issuedAt))
                .claim("issuedAtMillis", issuedAt)
                .setExpiration(new Date(issuedAt + expiration * 1000L));
        if (type != null && !type.isEmpty()) {
            builder.claim("type", type);
        }
        if (authVersion != null) {
            builder.claim("authVersion", authVersion);
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
}

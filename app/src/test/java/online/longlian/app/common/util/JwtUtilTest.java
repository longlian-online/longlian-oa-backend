package online.longlian.app.common.util;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {
    /** Two logins in the same millisecond receive independently revocable tokens. */
    @Test
    void shouldIssueUniqueTokensWithPreciseIssuedAt() {
        Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00.123Z"), ZoneOffset.UTC);
        JwtUtil jwt = new JwtUtil(clock);
        ReflectionTestUtils.setField(jwt, "secret", "test-jwt-secret-at-least-32-bytes-long");
        ReflectionTestUtils.setField(jwt, "expiration", 60);
        jwt.init();
        String first = jwt.generateToken(1L, "user");
        String second = jwt.generateToken(1L, "user");
        assertThat(first).isNotEqualTo(second);
        assertThat(jwt.parseToken(first).get("issuedAtMillis", Long.class)).isEqualTo(clock.millis());
        assertThat(jwt.parseToken(first).getId()).isNotEqualTo(jwt.parseToken(second).getId());
        assertThat(jwt.getRemainingTimeSeconds(first)).isEqualTo(60);
    }
}

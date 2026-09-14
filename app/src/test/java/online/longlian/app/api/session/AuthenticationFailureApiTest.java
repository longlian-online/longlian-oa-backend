package online.longlian.app.api.session;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.TokenBlacklistService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.doThrow;

class AuthenticationFailureApiTest extends BaseApiTest {
    @Value("${jwt.secret}")
    private String secret;
    @SpyBean
    private TokenBlacklistService blacklist;

    /** Clients can distinguish expired, revoked and invalid credentials without changing the legacy error code. */
    @Test
    void shouldDistinguishAuthenticationFailures() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        authRequest(token).delete("/admin/session").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String expired = Jwts.builder().setSubject("1").claim("type", "admin")
                .setExpiration(Date.from(Instant.now().minusSeconds(60)))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        String revokedMessage = failureMessage(token);
        String expiredMessage = failureMessage(expired);
        String invalidMessage = failureMessage("invalid.token");
        assertThat(revokedMessage).isNotEqualTo(expiredMessage).isNotEqualTo(invalidMessage);
        assertThat(expiredMessage).isNotEqualTo(invalidMessage);
    }

    /** Database failure returns a system error and keeps private exception details out of the response. */
    @Test
    void shouldReportUnavailableAuthenticationInfrastructure() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        doThrow(new IllegalStateException("private database details")).when(blacklist).isBlacklisted(token);
        String response = authRequest(token).get("/admin/admins/").then().statusCode(200)
                .body("code", equalTo(ResultCode.FAIL.getCode())).extract().asString();
        assertThat(response).doesNotContain("private database details", token);
    }

    private String failureMessage(String token) {
        return authRequest(token).get("/admin/admins/").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode())).extract().path("msg");
    }
}

package online.longlian.app.api.app;

import io.jsonwebtoken.Claims;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.JwtUtil;
import online.longlian.app.service.impl.TokenRevocationStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

public class SessionApiTest extends BaseApiTest {

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private StringRedisTemplate stringRedis;

    @Test
    void shouldIssueShortLivedAccessTokenAndRefreshToken() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");

        Session session = login("testuser");
        Claims access = jwtUtil.parseToken(session.token());

        assertThat(session.refreshToken()).isNotBlank().isNotEqualTo(session.token());
        assertThat(access.get("use", String.class)).isEqualTo("access");
        assertThat(access.get("sid", String.class)).isNotBlank();
        assertThat(access.getExpiration().getTime() - access.getIssuedAt().getTime()).isEqualTo(21_600_000L);
        userRequest(session.token()).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldKeepAdminTokenOnTheLongLifetime() {
        createAdmin(1L, "admin", "123456", "root");

        String token = adminLoginAs("admin", "123456");
        Claims access = jwtUtil.parseToken(token);

        assertThat(access.get("use", String.class)).isNull();
        assertThat(access.get("sid", String.class)).isNull();
        assertThat(access.getExpiration().getTime() - access.getIssuedAt().getTime()).isEqualTo(86_400_000L);
    }

    @Test
    void shouldRotateRefreshTokenAndReplayItWithinGrace() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");

        Response rotated = refresh(session.refreshToken());
        rotated.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue())
                .body("data.refreshToken", not(equalTo(session.refreshToken())));
        String access = rotated.jsonPath().getString("data.token");
        String refreshToken = rotated.jsonPath().getString("data.refreshToken");

        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", equalTo(access))
                .body("data.refreshToken", equalTo(refreshToken));
        userRequest(session.token()).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(access).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldRevokeDeviceWhenRotatedRefreshTokenIsReusedAfterGrace() throws Exception {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        Response rotated = refresh(session.refreshToken());
        String latest = rotated.jsonPath().getString("data.refreshToken");

        Thread.sleep(1_500);

        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        refresh(latest).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldLogoutOnlyTheCurrentDevice() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session first = login("testuser");
        Session second = login("testuser");

        userRequest(first.token()).delete("/app/session/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        userRequest(first.token()).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        refresh(first.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        userRequest(second.token()).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        refresh(second.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue());
    }

    @Test
    void shouldRejectRefreshAfterPasswordChange() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");

        userRequest(session.token()).body(Map.of("oldPassword", "123456", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        userRequest(session.token()).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectUnknownBlankAndAccessTokenRefresh() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");

        refresh("not-a-session").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().body(Map.of("refreshToken", "")).post("/app/session/refresh")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        refresh(session.token()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectRefreshUsePresentedAsAccessToken() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String forged = jwtUtil.generateToken(1L, "user", "refresh", "sid", 900);

        userRequest(forged).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectRefreshWhenUserIsDisabled() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = 1");

        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRotateTwiceAndTolerateCorruptRefreshIndexes() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        String second = refresh(session.refreshToken()).jsonPath().getString("data.refreshToken");
        Response third = refresh(second);
        third.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String current = third.jsonPath().getString("data.refreshToken");
        String sessionId = jwtUtil.parseToken(third.jsonPath().getString("data.token")).get("sid", String.class);

        stringRedis.opsForValue().set(RedisConstants.REFRESH_REVOKED + "1", "nope");
        Response afterNoise = refresh(current);
        afterNoise.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        current = afterNoise.jsonPath().getString("data.refreshToken");

        stringRedis.opsForValue().set(RedisConstants.REFRESH_SESSION + sessionId, "not-json");
        refresh(current).then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldIgnoreAnotherUsersRefreshSessionOnLogout() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "other", "123456", "other@example.com");
        Session owner = login("owner");
        String sessionId = jwtUtil.parseToken(owner.token()).get("sid", String.class);
        String forged = jwtUtil.generateToken(2L, "user", "access", sessionId, 21_600);

        userRequest(forged).delete("/app/session/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        refresh(owner.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldLogoutAccessTokenWhoseRefreshSessionIsAlreadyGone() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        String token = jwtUtil.generateToken(1L, "user", "access", "missing-session", 21_600);
        stringRedis.opsForSet().add(RedisConstants.REFRESH_USER + "1", "missing-session");

        userRequest(token).delete("/app/session/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(session.token()).body(Map.of("oldPassword", "123456", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldRejectRefreshAfterUserDisappears() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        jdbcTemplate.update("DELETE FROM `user` WHERE id = 1");

        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectRefreshTokenThatDoesNotMatchItsSession() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        Session session = login("testuser");
        String sessionId = jwtUtil.parseToken(session.token()).get("sid", String.class);
        stringRedis.opsForValue().set(
                RedisConstants.REFRESH_HASH + TokenRevocationStore.digest("foreign-token"), sessionId);

        refresh("foreign-token").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        refresh(session.refreshToken()).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    private Session login(String username) {
        Response response = request().body(Map.of("username", username, "password", "123456"))
                .post("/app/session/pwd");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue())
                .body("data.refreshToken", notNullValue());
        return new Session(response.jsonPath().getString("data.token"),
                response.jsonPath().getString("data.refreshToken"));
    }

    private Response refresh(String refreshToken) {
        return request().body(Map.of("refreshToken", refreshToken)).post("/app/session/refresh");
    }

    private record Session(String token, String refreshToken) {
    }
}

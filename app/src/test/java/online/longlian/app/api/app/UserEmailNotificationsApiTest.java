package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class UserEmailNotificationsApiTest extends BaseApiTest {
    private static final String ENDPOINT = "/app/user/config/email-notifications";

    @Test
    void shouldToggleEmailNotificationsWithoutAnOrganizationAndPreserveOtherConfig() {
        createTestUser(1L, "emailprefs", "123456", "emailprefs@example.com");
        assertThat(config(1L)).isEqualTo(new JSONObject());
        jdbcTemplate.update("UPDATE `user` SET config = ? WHERE id = 1",
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false}}");
        String token = loginAs("emailprefs", "123456");
        userRequest(token).get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.enabled", equalTo(true));

        setEnabled(token, false);
        assertThat(config(1L)).isEqualTo(JSON.parseObject(
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":false}"));
        userRequest(token).get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.enabled", equalTo(false));

        setEnabled(token, true);
        setEnabled(token, true);
        assertThat(config(1L)).isEqualTo(JSON.parseObject(
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":true}"));
        userRequest(token).get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.enabled", equalTo(true));
    }

    @Test
    void shouldOnlyUpdateTheAuthenticatedUser() {
        createTestUser(1L, "emailself", "123456", "emailself@example.com");
        createTestUser(2L, "emailother", "123456", "emailother@example.com");
        String token = loginAs("emailself", "123456");

        userRequest(token).body(Map.of("enabled", false, "userId", "2"))
                .put(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(config(1L).getBoolean("emailNotificationsEnabled")).isFalse();
        assertThat(config(2L)).isEqualTo(new JSONObject());
        String otherToken = loginAs("emailother", "123456");
        userRequest(otherToken).queryParam("userId", "1").get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.enabled", equalTo(true));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"enabled\":null}"})
    void shouldRejectMissingSwitchWithoutChangingConfig(String body) {
        createTestUser(1L, "emailinvalid", "123456", "emailinvalid@example.com");
        String token = loginAs("emailinvalid", "123456");

        userRequest(token).body(body).put(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(config(1L)).isEqualTo(new JSONObject());
    }

    @Test
    void shouldRequireUserAuthentication() {
        request().body(Map.of("enabled", false)).put(ENDPOINT)
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    private void setEnabled(String token, boolean enabled) {
        userRequest(token).body(Map.of("enabled", enabled)).put(ENDPOINT)
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", equalTo(null));
    }

    private JSONObject config(long userId) {
        return JSON.parseObject(jdbcTemplate.queryForObject(
                "SELECT config FROM `user` WHERE id = ?", String.class, userId));
    }
}

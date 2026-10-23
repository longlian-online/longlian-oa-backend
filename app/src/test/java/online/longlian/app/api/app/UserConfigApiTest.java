package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

class UserConfigApiTest extends BaseApiTest {
    private static final String ENDPOINT = "/app/user/config";

    @Test
    void shouldReplaceAndPatchCompleteConfigWithoutAnOrganization() {
        createTestUser(1L, "configuser", "123456", "configuser@example.com");
        String token = loginAs("configuser", "123456");
        userRequest(token).get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.config", equalTo(Map.of()));

        userRequest(token).body(Map.of("config", Map.of(
                        "theme", "dark",
                        "other", Map.of("enabled", false),
                        "emailNotificationsEnabled", true)))
                .put(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", nullValue());
        assertThat(config(1L)).isEqualTo(JSON.parseObject(
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":true}"));

        userRequest(token).body(Map.of("config", Map.of("emailNotificationsEnabled", false)))
                .patch(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(config(1L)).isEqualTo(JSON.parseObject(
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":false}"));
    }
    @Test
    void shouldDeleteOnlyExplicitNullFieldsWhenPatchingConfig() {
        createTestUser(1L, "configdelete", "123456", "configdelete@example.com");
        jdbcTemplate.update("UPDATE `user` SET config = ? WHERE id = 1",
                "{\"theme\":\"dark\",\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":false}");

        jdbcTemplate.update(
                "UPDATE `user` SET config = JSON_MERGE_PATCH(config, JSON_EXTRACT(?, '$')) WHERE id = 1",
                "{\"theme\":null}");

        assertThat(config(1L)).isEqualTo(JSON.parseObject(
                "{\"other\":{\"enabled\":false},\"emailNotificationsEnabled\":false}"));
    }


    @Test
    void shouldReplaceConfigForOnlyTheAuthenticatedUser() {
        createTestUser(1L, "configself", "123456", "configself@example.com");
        createTestUser(2L, "configother", "123456", "configother@example.com");
        String token = loginAs("configself", "123456");
        userRequest(token).body(Map.of("config", Map.of("emailNotificationsEnabled", false), "userId", "2"))
                .put(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(config(1L).getBoolean("emailNotificationsEnabled")).isFalse();
        assertThat(config(2L)).isEqualTo(new JSONObject());
        String otherToken = loginAs("configother", "123456");
        userRequest(otherToken).queryParam("userId", "1").get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.config.emailNotificationsEnabled", nullValue());
    }

    @Test
    void shouldRejectInvalidConfigPayloadsWithoutChangingStoredConfig() {
        createTestUser(1L, "configinvalid", "123456", "configinvalid@example.com");
        jdbcTemplate.update("UPDATE `user` SET config = ? WHERE id = 1", "{\"theme\":\"dark\"}");
        String token = loginAs("configinvalid", "123456");

        for (String body : new String[]{"{}", "{\"config\":null}", "[]", "\"dark\""}) {
            userRequest(token).body(body).put(ENDPOINT).then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
            userRequest(token).body(body).patch(ENDPOINT).then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        }
        assertThat(config(1L)).isEqualTo(JSON.parseObject("{\"theme\":\"dark\"}"));
    }

    @Test
    void shouldRequireUserAuthentication() {
        request().get(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().body(Map.of("config", Map.of())).put(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().body(Map.of("config", Map.of())).patch(ENDPOINT).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    private JSONObject config(long userId) {
        return JSON.parseObject(jdbcTemplate.queryForObject(
                "SELECT config FROM `user` WHERE id = ?", String.class, userId));
    }
}

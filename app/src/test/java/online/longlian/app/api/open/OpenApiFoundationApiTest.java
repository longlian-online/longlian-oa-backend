package online.longlian.app.api.open;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.startsWith;

/**
 * 开放接口地基的端到端验证：密钥管理、开放信封、认证失败语义、游标分页与密码重置吊销。
 */
public class OpenApiFoundationApiTest extends BaseApiTest {

    private static final long USER_ID = 20001L;
    private static final long ORG_ID = 21000L;
    private static final long MEMBER_ID = 22000L;

    private String createKeyForUser(String name) {
        createUserWithOrganization(USER_ID, "openuser", "123456", "open@example.com", MEMBER_ID, ORG_ID, "ORG_USER");
        String token = loginAs("openuser", "123456");
        Response response = authRequest(token)
                .body(Map.of("name", name))
                .post("/app/openapi-keys");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.id", notNullValue())
                .body("data.key", startsWith("oa_"));
        return response.jsonPath().getString("data.key");
    }

    private void insertProject(long id, String title, long creatorId, long orgId, String updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, 0, ?, 1, 1, ?, NOW(), ?)",
                id, orgId, title, creatorId, updatedAt);
    }

    private Response openGet(String path, String apiKey) {
        var spec = given();
        if (apiKey != null) {
            spec.header("X-Api-Key", apiKey);
        }
        return spec.get(path);
    }

    @Test
    void shouldCreateKeyAndListGroupsWithoutPlaintext() {
        String key = createKeyForUser("脚本A");
        String token = loginAs("openuser", "123456");

        Response list = authRequest(token).get("/app/openapi-keys");
        list.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(1))
                .body("data[0].orgId", equalTo((int) ORG_ID))
                .body("data[0].orgName", notNullValue())
                .body("data[0].maxKeys", equalTo(2))
                .body("data[0].keys", hasSize(1))
                .body("data[0].keys[0].name", equalTo("脚本A"))
                .body("data[0].keys[0].createdAt", notNullValue())
                .body("data[0].keys[0].lastUsedAt", nullValue());
        // 列表不回显明文
        org.assertj.core.api.Assertions.assertThat(list.body().asString()).doesNotContain(key);
    }

    @Test
    void shouldFailCreateWhenOrgQuotaReached() {
        createUserWithOrganization(USER_ID, "openuser", "123456", "open@example.com", MEMBER_ID, ORG_ID, "ORG_USER");
        String token = loginAs("openuser", "123456");

        authRequest(token).body(Map.of("name", "a")).post("/app/openapi-keys").then().body("code", equalTo(0));
        authRequest(token).body(Map.of("name", "b")).post("/app/openapi-keys").then().body("code", equalTo(0));
        authRequest(token).body(Map.of("name", "c")).post("/app/openapi-keys").then()
                .statusCode(200)
                .body("code", equalTo(5))
                .body("msg", notNullValue());
    }

    @Test
    void shouldFailOpenWithoutKey() {
        openGet("/open/v1/projects", null).then()
                .statusCode(401)
                .body("code", equalTo(10001))
                .body("error", equalTo("OPEN_UNAUTHENTICATED"))
                .body("requestId", notNullValue());
    }

    @Test
    void shouldFailOpenWithUnknownKey() {
        openGet("/open/v1/projects", "oa_does_not_exist").then()
                .statusCode(401)
                .body("code", equalTo(10001))
                .body("error", equalTo("OPEN_UNAUTHENTICATED"));
    }

    @Test
    void shouldRejectStationTokenOnOpenSurface() {
        createUserWithOrganization(USER_ID, "openuser", "123456", "open@example.com", MEMBER_ID, ORG_ID, "ORG_USER");
        String token = loginAs("openuser", "123456");

        given().header("Authorization", "Bearer " + token).get("/open/v1/projects").then()
                .statusCode(403)
                .body("code", equalTo(10003))
                .body("error", equalTo("OPEN_FORBIDDEN"));
    }

    @Test
    void shouldReturnNotFoundEnvelopeForUnknownOpenPath() {
        String key = createKeyForUser("脚本A");

        openGet("/open/v1/not-exist", key).then()
                .statusCode(404)
                .body("code", equalTo(10004))
                .body("error", equalTo("OPEN_NOT_FOUND"));
    }

    @Test
    void shouldPageProjectsByUpdatedAtCursor() {
        String key = createKeyForUser("脚本A");
        insertProject(301L, "最早", USER_ID, ORG_ID, "2026-01-01 08:00:00");
        insertProject(302L, "居中", USER_ID, ORG_ID, "2026-01-02 08:00:00");
        insertProject(303L, "最新", USER_ID, ORG_ID, "2026-01-03 08:00:00");

        Response page1 = given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .get("/open/v1/projects");
        page1.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", hasSize(2))
                .body("data.list[0].id", equalTo(301))
                .body("data.list[1].id", equalTo(302))
                .body("data.hasMore", equalTo(true))
                .body("data.nextCursor", notNullValue())
                .body("error", nullValue())
                .body("requestId", notNullValue());

        String cursor = page1.jsonPath().getString("data.nextCursor");
        given().header("X-Api-Key", key)
                .queryParam("limit", 2)
                .queryParam("cursor", cursor)
                .get("/open/v1/projects").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].id", equalTo(303))
                .body("data.hasMore", equalTo(false))
                .body("data.nextCursor", nullValue());
    }

    @Test
    void shouldMarkCreatorAndFilterOtherOrgProjects() {
        String key = createKeyForUser("脚本A");
        insertProject(401L, "本组织", USER_ID, ORG_ID, "2026-01-01 08:00:00");
        insertProject(402L, "别的组织", USER_ID, ORG_ID + 1, "2026-01-02 08:00:00");

        given().header("X-Api-Key", key).get("/open/v1/projects").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].id", equalTo(401))
                .body("data.list[0].creator", equalTo(true))
                .body("data.list[0].status", equalTo("进行中"));
    }

    @Test
    void shouldRejectMalformedCursorAndUpdatedSince() {
        String key = createKeyForUser("脚本A");

        given().header("X-Api-Key", key)
                .queryParam("cursor", "not-base64!!")
                .get("/open/v1/projects").then()
                .statusCode(400)
                .body("code", equalTo(10002))
                .body("error", equalTo("OPEN_PARAM_ERROR"));

        given().header("X-Api-Key", key)
                .queryParam("updatedSince", "2026/01/01")
                .get("/open/v1/projects").then()
                .statusCode(400)
                .body("code", equalTo(10002));
    }

    @Test
    void shouldRevokeKeyAndBlockOpenAccessImmediately() {
        String key = createKeyForUser("脚本A");
        openGet("/open/v1/projects", key).then().statusCode(200);

        String token = loginAs("openuser", "123456");
        Response list = authRequest(token).get("/app/openapi-keys");
        long keyId = list.jsonPath().getLong("data[0].keys[0].id");

        authRequest(token).delete("/app/openapi-keys/" + keyId).then()
                .statusCode(200)
                .body("code", equalTo(0));

        openGet("/open/v1/projects", key).then()
                .statusCode(401)
                .body("code", equalTo(10001));
    }

    @Test
    void shouldRevokeAllKeysAfterPasswordReset() {
        String key = createKeyForUser("脚本A");

        createEmailVerifyOTP("A1B2C3", USER_ID, "open@example.com", EmailVerifyBusinessType.FORGOT_PASSWORD);
        given().contentType(io.restassured.http.ContentType.JSON)
                .body(Map.of("email", "open@example.com", "code", "A1B2C3", "password", "NewPass6Aa"))
                .put("/app/user/password").then()
                .statusCode(200)
                .body("code", equalTo(0));

        openGet("/open/v1/projects", key).then()
                .statusCode(401)
                .body("code", equalTo(10001));
    }
}

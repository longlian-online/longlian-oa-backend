package online.longlian.app.api.open;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;

/**
 * 开放面接口测试公共件：建用户/组织、登录并签发一把 OpenAPI 密钥。
 * ID 用大段固定值（企划 41000+、项目 42000+、任务 43000+、成员 44000+）避免与其它用例相撞。
 */
public abstract class OpenApiFoundationApiTestSupport extends BaseApiTest {

    protected String createKeyForUser(long userId, String username, long memberRowId, long orgId) {
        createUserWithOrganization(userId, username, "123456", username + "@example.com", memberRowId, orgId, "ORG_USER");
        String token = loginAs(username, "123456");
        Response response = authRequest(token)
                .body(Map.of("name", "脚本"))
                .post("/app/openapi-keys");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
        return response.jsonPath().getString("data.key");
    }
}

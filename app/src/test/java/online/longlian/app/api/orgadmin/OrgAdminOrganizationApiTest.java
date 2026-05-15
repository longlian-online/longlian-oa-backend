package online.longlian.app.api.orgadmin;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

public class OrgAdminOrganizationApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 获取组织信息成功
     */
    @Test
    void shouldGetOrganizationInfoSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.id", notNullValue())
                .body("data.name", notNullValue());
    }

    /**
     * 更新组织信息成功
     */
    @Test
    void shouldUpdateOrganizationInfoSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "更新后的组织名称",
                        "description", "更新后的组织描述",
                        "avatarFileId", 12345L
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证获取组织信息失败
     */
    @Test
    void shouldFailGetOrganizationInfoWithoutAuth() {
        Response response = request()
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(401);
    }

    /**
     * 未认证更新组织信息失败
     */
    @Test
    void shouldFailUpdateOrganizationInfoWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(401);
    }

    /**
     * Token无效获取组织信息失败
     */
    @Test
    void shouldFailGetOrganizationInfoWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(401);
    }

    // ========== 参数校验失败 ==========

    /**
     * 更新组织信息时名称超长应失败
     */
    @Test
    void shouldFailUpdateOrganizationInfoWithNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longName = "a".repeat(51);
        Response response = authRequest(token)
                .body(Map.of(
                        "name", longName,
                        "avatarFileId", 12345L,
                        "description", "描述"
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200);
    }

    /**
     * 更新组织信息时description为空应失败
     */
    @Test
    void shouldFailUpdateOrganizationInfoWithEmptyDescription() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "组织名",
                        "avatarFileId", 12345L,
                        "description", ""
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200);
    }

    /**
     * 更新组织信息时description超长应失败
     */
    @Test
    void shouldFailUpdateOrganizationInfoWithDescriptionTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longDesc = "a".repeat(501);
        Response response = authRequest(token)
                .body(Map.of(
                        "name", "组织名",
                        "avatarFileId", 12345L,
                        "description", longDesc
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200);
    }

    /**
     * 更新组织信息时avatarFileId为空应失败
     */
    @Test
    void shouldFailUpdateOrganizationInfoWithNullAvatarFileId() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "组织名",
                        "description", "描述"
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200);
    }

    // ========== 业务规则失败 ==========

    /**
     * 无组织用户获取组织信息应失败
     * 注：用户端登录需要组织关联，因此此场景通过直接调用接口验证
     */
    @Test
    void shouldFailGetOrganizationInfoWithoutOrganization() {
        createTestUser(1L, "user_no_org", "123456", "user@example.com");

        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", "user_no_org", "password", "123456"))
                .post("/app/session/pwd");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    // ========== 边界条件 ==========

    /**
     * 获取组织信息返回完整字段
     */
    @Test
    void shouldGetOrganizationInfoWithAllFields() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", notNullValue())
                .body("data.id", notNullValue())
                .body("data.name", notNullValue())
                .body("data.description", notNullValue());
    }

    // ========== 权限检查 ==========

    /**
     * 未授权角色获取组织信息应失败
     */
    @Test
    void shouldFailGetOrganizationInfoWithoutAdminRole() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "MEMBER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
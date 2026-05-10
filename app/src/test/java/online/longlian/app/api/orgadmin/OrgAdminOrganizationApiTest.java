package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminOrganizationApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 获取组织信息成功
     */
    @Test
    void shouldGetOrganizationInfo() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 更新组织信息成功
     */
    @Test
    void shouldUpdateOrganizationInfo() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "新组织名称",
                        "description", "新的组织描述"
                ))
                .put("/orgadmin/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问组织信息接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .get("/orgadmin/organizations");

        response.then()
                .statusCode(401);
    }
}
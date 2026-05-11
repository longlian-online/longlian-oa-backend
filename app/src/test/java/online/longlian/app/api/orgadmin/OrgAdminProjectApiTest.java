package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminProjectApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 管理端分页查询企划列表成功
     */
    @Test
    void shouldListAdminProjects() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 启用/禁用企划成功
     */
    @Test
    void shouldChangeProjectStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问企划管理接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .post("/orgadmin/projects");

        response.then()
                .statusCode(401);
    }
}
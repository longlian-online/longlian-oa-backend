package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class ProjectTypeApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询企划类型列表成功
     */
    @Test
    void shouldListProjectTypes() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 创建企划类型成功
     */
    @Test
    void shouldCreateProjectType() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试类型",
                        "description", "这是一个测试类型"
                ))
                .post("/orgadmin/project-types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 启用/禁用企划类型成功
     */
    @Test
    void shouldChangeProjectTypeStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/project-types/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问企划类型管理接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .get("/orgadmin/project-types");

        response.then()
                .statusCode(401);
    }
}
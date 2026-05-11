package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class TaskTemplateApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询任务模板列表成功
     */
    @Test
    void shouldListTaskTemplates() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 获取任务模板详情成功
     */
    @Test
    void shouldGetTaskTemplateDetail() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 创建任务模板成功
     */
    @Test
    void shouldCreateTaskTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试模板",
                        "description", "这是一个测试模板",
                        "nodes", ""
                ))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 更新任务模板成功
     */
    @Test
    void shouldUpdateTaskTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "更新后的模板",
                        "description", "更新后的描述",
                        "nodes", ""
                ))
                .put("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 启用/禁用任务模板成功
     */
    @Test
    void shouldChangeTaskTemplateStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/task/template/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问任务模板管理接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(401);
    }
}
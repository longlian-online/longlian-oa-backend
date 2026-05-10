package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class BaseTaskApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询原子任务列表成功
     */
    @Test
    void shouldListBaseTasks() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 创建原子任务成功
     */
    @Test
    void shouldCreateBaseTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试原子任务",
                        "icon", "test-icon",
                        "description", "这是一个测试原子任务"
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 启用/禁用原子任务成功
     */
    @Test
    void shouldChangeBaseTaskStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问原子任务管理接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(401);
    }
}
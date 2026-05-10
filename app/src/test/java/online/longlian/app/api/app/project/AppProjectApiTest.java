package online.longlian.app.api.app.project;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class AppProjectApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询企划列表成功
     */
    @Test
    void shouldGetProjectList() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .get("/app/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 获取企划详情成功
     */
    @Test
    void shouldGetProjectDetail() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .get("/app/projects/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 获取企划类型列表成功
     */
    @Test
    void shouldGetProjectTypes() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .get("/app/projects/types");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 创建企划成功
     */
    @Test
    void shouldCreateProject() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "测试企划",
                        "typeId", 1,
                        "description", "这是一个测试企划"
                ))
                .post("/app/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 编辑企划成功
     */
    @Test
    void shouldUpdateProject() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "更新后的企划标题",
                        "typeId", 1,
                        "description", "更新后的描述"
                ))
                .put("/app/projects/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 添加企划到工坊成功
     */
    @Test
    void shouldAddProjectToWorkshop() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .post("/app/projects/1/workshop");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 从工坊移除企划成功
     */
    @Test
    void shouldRemoveProjectFromWorkshop() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .delete("/app/projects/1/workshop");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问企划列表
     */
    @Test
    void shouldGetProjectListWithoutAuth() {
        Response response = request()
                .get("/app/projects");

        response.then()
                .statusCode(200);
    }

    /**
     * 未认证创建企划失败
     */
    @Test
    void shouldFailCreateProjectWithoutAuth() {
        Response response = request()
                .body(Map.of(
                        "title", "测试企划",
                        "typeId", 1
                ))
                .post("/app/projects");

        response.then()
                .statusCode(401);
    }

    /**
     * 未认证访问企划详情失败
     */
    @Test
    void shouldFailGetProjectDetailWithoutAuth() {
        Response response = request()
                .get("/app/projects/1");

        response.then()
                .statusCode(401);
    }

    // ========== 参数校验 ==========

    /**
     * 创建企划时标题为空校验失败
     */
    @Test
    void shouldFailCreateProjectWithEmptyTitle() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "",
                        "typeId", 1
                ))
                .post("/app/projects");

        response.then()
                .statusCode(400);
    }
}
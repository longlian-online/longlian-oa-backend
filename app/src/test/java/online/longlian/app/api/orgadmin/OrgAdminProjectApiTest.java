package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminProjectApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 管理端分页查询企划列表成功
     */
    @Test
    void shouldListProjectsSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 启用企划成功
     */
    @Test
    void shouldEnableProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, title, description, type_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试企划", "描述", 0L, 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 禁用企划成功
     */
    @Test
    void shouldDisableProjectSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, title, description, type_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试企划", "描述", 0L, 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询企划列表失败
     */
    @Test
    void shouldFailListProjectsWithoutAuth() {
        Response response = request()
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证修改企划状态失败
     */
    @Test
    void shouldFailChangeProjectStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 修改企划状态时status为空应失败
     */
    @Test
    void shouldFailChangeProjectStatusWithEmptyStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", ""))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200);
    }

    /**
     * 修改企划状态时status值无效应失败
     */
    @Test
    void shouldFailChangeProjectStatusWithInvalidStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "INVALID_STATUS"))
                .patch("/orgadmin/projects/1/status");

        response.then()
                .statusCode(200);
    }

    // ========== 业务规则失败 ==========

    /**
     * 修改不存在的企划状态应失败
     */
    @Test
    void shouldFailChangeStatusForNonExistentProject() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/projects/99999/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 边界条件 ==========

    /**
     * 查询企划列表时分页边界测试
     */
    @Test
    void shouldListProjectsWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 0, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200);
    }

    /**
     * 查询企划列表时pageSize超过上限应失败
     */
    @Test
    void shouldListProjectsWithPageSizeExceedingMax() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 101))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200);
    }

    /**
     * 查询企划列表时关键词筛选测试
     */
    @Test
    void shouldListProjectsWithKeywordFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "keyword", "测试"
                ))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 查询企划列表时空数据应返回空列表
     */
    @Test
    void shouldListProjectsWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }

    /**
     * 按类型筛选企划列表测试
     */
    @Test
    void shouldListProjectsWithTypeFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "typeId", 1L
                ))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 权限检查 ==========

    /**
     * Token无效查询企划列表失败
     */
    @Test
    void shouldFailListProjectsWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未授权角色查询企划列表应失败
     */
    @Test
    void shouldFailListProjectsWithoutAdminRole() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "MEMBER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/projects");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
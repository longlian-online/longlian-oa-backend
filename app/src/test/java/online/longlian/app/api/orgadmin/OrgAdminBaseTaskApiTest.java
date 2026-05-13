package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminBaseTaskApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询原子任务列表成功
     */
    @Test
    void shouldListBaseTasksSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 创建原子任务成功
     */
    @Test
    void shouldCreateBaseTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试原子任务",
                        "description", "这是一个测试用的原子任务",
                        "iconFileId", 12345L,
                        "metaSchema", "[]"
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 创建原子任务时description超长应失败
     */
    @Test
    void shouldFailCreateBaseTaskWithDescriptionTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longDesc = "a".repeat(501);
        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试任务",
                        "description", longDesc
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(400);
    }

    /**
     * 启用原子任务成功
     */
    @Test
    void shouldEnableBaseTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, reference_count, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "禁用状态任务", "描述", 0L, "[]", 0, "DISABLED", 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 禁用原子任务成功
     */
    @Test
    void shouldDisableBaseTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, reference_count, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "启用状态任务", "描述", 0L, "[]", 0, "ENABLED", 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询原子任务列表失败
     */
    @Test
    void shouldFailListBaseTasksWithoutAuth() {
        Response response = request()
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(401);
    }

    /**
     * 未认证创建原子任务失败
     */
    @Test
    void shouldFailCreateBaseTaskWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(401);
    }

    /**
     * 未认证修改任务状态失败
     */
    @Test
    void shouldFailChangeBaseTaskStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(401);
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建原子任务时名称为空应失败
     */
    @Test
    void shouldFailCreateBaseTaskWithEmptyName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("name", ""))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(400);
    }

    /**
     * 创建原子任务时名称超长应失败
     */
    @Test
    void shouldFailCreateBaseTaskWithNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longName = "a".repeat(101);
        Response response = authRequest(token)
                .body(Map.of("name", longName))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(400);
    }

    /**
     * 修改状态时status为空应失败
     */
    @Test
    void shouldFailChangeBaseTaskStatusWithEmptyStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", ""))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(400);
    }

    // ========== 业务规则失败 ==========

    /**
     * 修改不存在的原子任务状态应失败
     */
    @Test
    void shouldFailChangeStatusForNonExistentTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/base/99999/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 边界条件 ==========

    /**
     * 查询列表时分页边界测试
     */
    @Test
    void shouldListBaseTasksWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 0, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(400);
    }

    /**
     * 查询列表时pageSize超过上限应失败
     */
    @Test
    void shouldListBaseTasksWithPageSizeExceedingMax() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 101))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(400);
    }

    /**
     * 查询列表时pageSize为0应返回空列表
     */
    @Test
    void shouldListBaseTasksWithZeroPageSize() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 0))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 查询列表时关键词筛选测试
     */
    @Test
    void shouldListBaseTasksWithKeywordFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "keyword", "测试"
                ))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 查询列表时空数据应返回空列表
     */
    @Test
    void shouldListBaseTasksWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }

    // ========== 权限检查 ==========

    /**
     * 未授权角色查询原子任务列表应失败
     */
    @Test
    void shouldFailListBaseTasksWithoutAdminRole() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "MEMBER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
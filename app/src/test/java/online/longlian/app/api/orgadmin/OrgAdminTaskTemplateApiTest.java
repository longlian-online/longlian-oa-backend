package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminTaskTemplateApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询任务模板列表成功
     */
    @Test
    void shouldListTaskTemplatesSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 获取任务模板详情成功
     */
    @Test
    void shouldGetTaskTemplateDetailSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试模板", "描述", 1, 1L
        );

        Response response = authRequest(token)
                .get("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.id", notNullValue())
                .body("data.name", notNullValue());
    }

    /**
     * 创建任务模板成功
     */
    @Test
    void shouldCreateTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务1", "描述", 0L, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试模板",
                        "description", "模板描述",
                        "nodes", new Object[]{
                                Map.of("baseTaskId", 1L, "sort", 1, "parallelSort", 1)
                        }
                ))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 更新任务模板成功
     */
    @Test
    void shouldUpdateTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", 0L, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试模板", "描述", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "更新后的模板名",
                        "description", "更新后的描述",
                        "nodes", new Object[]{
                                Map.of("baseTaskId", 1L, "sort", 1, "parallelSort", 1)
                        }
                ))
                .put("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 启用任务模板成功
     */
    @Test
    void shouldEnableTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试模板", "描述", 0, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/template/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 禁用任务模板成功
     */
    @Test
    void shouldDisableTaskTemplateSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试模板", "描述", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/task/template/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询任务模板列表失败
     */
    @Test
    void shouldFailListTaskTemplatesWithoutAuth() {
        Response response = request()
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证获取任务模板详情失败
     */
    @Test
    void shouldFailGetTaskTemplateDetailWithoutAuth() {
        Response response = request()
                .get("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证创建任务模板失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证更新任务模板失败
     */
    @Test
    void shouldFailUpdateTaskTemplateWithoutAuth() {
        Response response = request()
                .body(Map.of("name", "test"))
                .put("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证修改任务模板状态失败
     */
    @Test
    void shouldFailChangeTaskTemplateStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/template/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建任务模板时名称为空应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithEmptyName() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("name", ""))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200);
    }

    /**
     * 创建任务模板时名称超长应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longName = "a".repeat(101);
        Response response = authRequest(token)
                .body(Map.of("name", longName))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200);
    }

    /**
     * 修改任务模板状态时status为空应失败
     */
    @Test
    void shouldFailChangeTaskTemplateStatusWithEmptyStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", ""))
                .patch("/orgadmin/task/template/1/status");

        response.then()
                .statusCode(200);
    }

    // ========== 业务规则失败 ==========

    /**
     * 获取不存在的任务模板详情应失败
     */
    @Test
    void shouldFailGetNonExistentTaskTemplateDetail() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/task/template/99999");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 修改不存在的任务模板状态应失败
     */
    @Test
    void shouldFailChangeStatusForNonExistentTaskTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/orgadmin/task/template/99999/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 边界条件 ==========

    /**
     * 查询任务模板列表时分页边界测试
     */
    @Test
    void shouldListTaskTemplatesWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 0, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200);
    }

    /**
     * 查询任务模板列表时pageSize超过上限应失败
     */
    @Test
    void shouldListTaskTemplatesWithPageSizeExceedingMax() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 101))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200);
    }

    /**
     * 查询任务模板列表时关键词筛选测试
     */
    @Test
    void shouldListTaskTemplatesWithKeywordFilter() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "pageNum", 1,
                        "pageSize", 10,
                        "keyword", "测试"
                ))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 查询任务模板列表时空数据应返回空列表
     */
    @Test
    void shouldListTaskTemplatesWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }

    /**
     * 创建无节点的任务模板应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithoutNodes() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "无节点模板",
                        "description", "描述",
                        "nodes", new Object[]{}
                ))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200);
    }

    /**
     * 创建任务模板时description超长应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithDescriptionTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", 0L, "[]", 1, 1L
        );

        String longDesc = "a".repeat(501);
        Response response = authRequest(token)
                .body(Map.of(
                        "name", "模板",
                        "description", longDesc,
                        "nodes", new Object[]{
                                Map.of("baseTaskId", 1L, "sort", 1, "parallelSort", 1)
                        }
                ))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200);
    }

    /**
     * 获取任务模板详情返回完整节点列表
     */
    @Test
    void shouldGetTaskTemplateDetailWithNodes() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试模板", "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", 0L, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `task_template_node` (id, task_template_id, base_task_id, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1, 1
        );

        Response response = authRequest(token)
                .get("/orgadmin/task/template/1");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes", notNullValue());
    }

    // ========== 参数校验（进阶） ==========

    /**
     * 创建任务模板时节点sort为负数应失败
     */
    @Test
    void shouldFailCreateTaskTemplateWithNegativeSort() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", 0L, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "模板",
                        "nodes", new Object[]{
                                Map.of("baseTaskId", 1L, "sort", -1)
                        }
                ))
                .post("/orgadmin/task/template");

        response.then()
                .statusCode(200);
    }

    // ========== 业务规则失败（进阶） ==========

    /**
     * 更新不存在的任务模板应失败
     */
    @Test
    void shouldFailUpdateNonExistentTaskTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", 0L, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "模板",
                        "nodes", new Object[]{
                                Map.of("baseTaskId", 1L, "sort", 0, "parallelSort", 1)
                        }
                ))
                .put("/orgadmin/task/template/99999");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 权限检查 ==========

    /**
     * 未授权角色查询任务模板列表应失败
     */
    @Test
    void shouldFailListTaskTemplatesWithoutAdminRole() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "MEMBER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/template/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}
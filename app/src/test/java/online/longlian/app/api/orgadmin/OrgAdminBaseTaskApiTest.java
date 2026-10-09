package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class OrgAdminBaseTaskApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    @Test
    void shouldListEnabledAndDisabledBaseTasksWhenStatusIsOmitted() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createBaseTasksForStatusFiltering();
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(2))
                .body("data.list.id", containsInAnyOrder("1", "2"))
                .body("data.list.status", containsInAnyOrder("ENABLED", "DISABLED"));
    }

    @ParameterizedTest
    @CsvSource({"ENABLED, 1", "DISABLED, 2"})
    void shouldListOnlyBaseTasksMatchingExplicitStatus(String status, String expectedId) {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createBaseTasksForStatusFiltering();
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10, "status", status))
                .post("/orgadmin/task/base/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1))
                .body("data.list.id", contains(expectedId))
                .body("data.list.status", contains(status));
    }

    private void createBaseTasksForStatusFiltering() {
        createOrganization(2L, "其他组织");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, status, creator_id, meta_schema, deleted_at) VALUES " +
                        "(1, 1, '启用任务', 1, 1, '[]', NULL), " +
                        "(2, 1, '禁用任务', 0, 1, '[]', NULL), " +
                        "(3, 2, '其他组织任务', 0, 1, '[]', NULL), " +
                        "(4, 1, '已删除任务', 0, 1, '[]', NOW())"
        );
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
                        "icon", "Languages",
                        "submitFields", List.of(
                                Map.of("key", "summary", "label", "摘要", "type", "text",
                                        "required", true, "options", List.of()),
                                Map.of("key", "category", "label", "分类", "type", "select",
                                        "required", false, "options", List.of("设计", "开发")))
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].icon", equalTo("Languages"))
                .body("data.list[0].submitFields.key", contains("summary", "category"))
                .body("data.list[0].submitFields.label", contains("摘要", "分类"))
                .body("data.list[0].submitFields.type", contains("text", "select"))
                .body("data.list[0].submitFields.required", contains(true, false))
                .body("data.list[0].submitFields[0].options", empty())
                .body("data.list[0].submitFields[1].options", contains("设计", "开发"))
                .body("data.list[0]", not(hasKey("metaSchema")));
    }

    /**
     * 创建原子任务时Lucide图标组件名超长应失败
     */
    @Test
    void shouldFailCreateBaseTaskWithIconNameTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of(
                        "name", "测试任务",
                        "icon", "a".repeat(101),
                        "submitFields", List.of()
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
                        "description", longDesc,
                        "submitFields", List.of()
                ))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 启用原子任务成功
     */
    @Test
    void shouldEnableBaseTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "禁用状态任务", "描述", null, "[]", 0, 1L
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
                "INSERT INTO `base_task` (id, org_id, name, description, icon, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "启用状态任务", "描述", null, "[]", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/task/base/1/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1))
                .body("data.list[0].id", equalTo("1"))
                .body("data.list[0].status", equalTo("DISABLED"));
    }

    @Test
    void shouldDeleteUnreferencedBaseTaskAndHideItFromList() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        insertBaseTask(1L, 1L, "待删除");

        authRequest(token).delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("删除成功"));

        authRequest(token).body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0));
    }

    @Test
    void shouldFailDeleteBaseTaskReferencedByTemplate() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        insertBaseTask(1L, 1L, "被模板引用");
        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "模板", "描述", 1, 2, 1L);
        jdbcTemplate.update(
                "INSERT INTO `task_template_node` (id, task_template_id, base_task_id, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1, 0);

        authRequest(token).delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", equalTo("参数错误,该原子任务已被任务模板或项目任务节点引用，请改为禁用"));
    }

    @Test
    void shouldFailDeleteBaseTaskReferencedByItemNode() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        insertBaseTask(1L, 1L, "被项目引用");
        jdbcTemplate.update(
                "INSERT INTO `item_task_node` (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点", "[]", 1, 0);

        authRequest(token).delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailDeleteBaseTaskFromAnotherOrganization() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        insertBaseTask(1L, 2L, "其他组织任务");

        authRequest(token).delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()))
                .body("msg", equalTo("无权操作,无权操作该原子任务"));
    }

    @Test
    void shouldFailDeleteMissingBaseTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token).delete("/orgadmin/task/base/99999")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()))
                .body("msg", equalTo("数据不存在,原子任务不存在"));
    }

    @Test
    void shouldFailDeleteBaseTaskWithoutAuth() {
        request().delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldFailDeleteBaseTaskAsNonAdmin() {
        createUserWithOrganization(1L, "member", "123456", "member@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("member", "123456");
        insertBaseTask(1L, 1L, "普通成员任务");

        authRequest(token).delete("/orgadmin/task/base/1")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    private void insertBaseTask(long id, long orgId, String name) {
        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                id, orgId, name, "描述", "Camera", "[]", 1, 1L);
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
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
                .body(Map.of("name", "", "submitFields", List.of()))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
                .body(Map.of("name", longName, "submitFields", List.of()))
                .post("/orgadmin/task/base");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询列表时pageSize为0应返回参数错误
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
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
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
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("regular", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldRejectInvalidSubmitFieldDefinitions() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        Map<String, Object> text = Map.of("key", "summary", "label", "摘要", "type", "text",
                "required", false, "options", List.of());
        List<List<Map<String, Object>>> invalidFields = List.of(
                List.of(text, text),
                List.of(Map.of("key", "summary", "label", "摘要", "type", "unknown",
                        "required", false, "options", List.of())),
                List.of(Map.of("key", "category", "label", "分类", "type", "select",
                        "required", false, "options", List.of())),
                List.of(Map.of("key", "summary", "label", "摘要", "type", "text",
                        "required", false, "options", List.of("不适用"))),
                List.of(Map.of("key", "category", "label", "分类", "type", "select",
                        "required", true, "options", List.of("重复", "重复"))));

        for (List<Map<String, Object>> fields : invalidFields) {
            authRequest(token).body(Map.of("name", "测试任务", "submitFields", fields))
                    .post("/orgadmin/task/base").then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        }
        authRequest(token).body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/task/base/list").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", empty());
    }
}

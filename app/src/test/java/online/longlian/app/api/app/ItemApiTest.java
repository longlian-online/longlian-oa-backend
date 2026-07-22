package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class ItemApiTest extends BaseApiTest {

    // ========== 成功路径 ==========

    /**
     * 分页查询项目列表成功
     */
    @Test
    void shouldListItemsSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", notNullValue());
    }

    /**
     * 创建项目成功
     */
    @Test
    void shouldCreateItemSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "模板", "描述", 1, 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "测试项目",
                        "taskTemplateId", "1"
                ))
                .post("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 删除项目成功
     */
    @Test
    void shouldDeleteItemSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        Response response = authRequest(token)
                .delete("/app/projects/1/items/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 公布项目成功
     */
    @Test
    void shouldPublishItemSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 2, 1L
        );

        Response response = authRequest(token)
                .patch("/app/projects/1/items/1/publish");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 获取项目任务流成功
     */
    @Test
    void shouldGetItemTaskFlowSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `item_task_flow` (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流名称", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `item_task_node` (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO `task_instance` (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, null, 1
        );

        Response response = authRequest(token)
                .get("/app/item/1/flow");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue());

        jdbcTemplate.update("UPDATE `item` SET deleted_at = NOW() WHERE id = ?", 1L);

        authRequest(token)
                .get("/app/item/1/flow")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 获取不存在的项目任务流返回数据不存在
     */
    @Test
    void shouldFailGetItemTaskFlowForNonexistentItem() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/app/item/99999/flow");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    // ========== 认证失败 ==========

    /**
     * 未认证查询项目列表失败
     */
    @Test
    void shouldFailListItemWithoutAuth() {
        Response response = request()
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证创建项目失败
     */
    @Test
    void shouldFailCreateItemWithoutAuth() {
        Response response = request()
                .body(Map.of("title", "test"))
                .post("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证删除项目失败
     */
    @Test
    void shouldFailDeleteItemWithoutAuth() {
        Response response = request()
                .delete("/app/projects/1/items/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 未认证公布项目失败
     */
    @Test
    void shouldFailPublishItemWithoutAuth() {
        Response response = request()
                .patch("/app/projects/1/items/1/publish");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * Token无效查询项目列表失败
     */
    @Test
    void shouldFailListItemWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 参数校验失败 ==========

    /**
     * 创建项目时标题为空应失败
     */
    @Test
    void shouldFailCreateItemWithEmptyTitle() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("title", ""))
                .post("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 业务规则失败 ==========

    /**
     * 非企划创建者创建项目应失败
     */
    @Test
    void shouldFailCreateItemNotProjectCreator() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 999L
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "title", "测试项目",
                        "taskTemplateId", "1"
                ))
                .post("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /**
     * 查询不存在的企划项目列表应返回数据不存在
     */
    @Test
    void shouldFailListItemsForNonExistentProject() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/99999/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 删除不存在的项目应失败
     */
    @Test
    void shouldFailDeleteNonExistentItem() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        Response response = authRequest(token)
                .delete("/app/projects/1/items/99999");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 边界条件 ==========

    /**
     * 查询项目列表时分页边界测试
     */
    @Test
    void shouldListItemsWithPaginationBoundaries() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        Response response = authRequest(token)
                .queryParam("pageNum", 0)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 查询项目列表时空数据应返回空列表
     */
    @Test
    void shouldListItemsWithEmptyResult() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", hasSize(0))
                .body("data.total", equalTo(0));
    }
}

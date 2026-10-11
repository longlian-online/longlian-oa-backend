package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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

    /** 批量查询保留重复节点与禁用任务快照，部分任务缺失时拒绝新建项目。 */
    @Test
    void shouldPreserveRepeatedTaskSnapshotsAndRejectMissingTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '测试类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) VALUES (1, 1, 1, '测试企划', 'alias', '{}', 0, '说明', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id) VALUES (1, 1, '模板', 1, 1, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '禁用任务', '[]', 0, 1), (2, 1, '启用任务', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 2, 0, 1), (2, 1, 1, 1, 1), (3, 1, 2, 2, 1)");

        authRequest(token).body(Map.of("title", "批量快照项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long itemId = jdbcTemplate.queryForObject("SELECT id FROM item WHERE title = '批量快照项目'", Long.class);
        authRequest(token).get("/app/item/" + itemId + "/flow").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes.baseTaskId", equalTo(java.util.List.of("2", "1", "2")))
                .body("data.nodes.name", equalTo(java.util.List.of("启用任务", "禁用任务", "启用任务")));

        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (4, 1, 999, 3, 1)");
        authRequest(token).body(Map.of("title", "缺失任务项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForList("SELECT title FROM item", String.class)).containsExactly("批量快照项目");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_task_node", Long.class)).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject("SELECT ref_count FROM task_template WHERE id=1", Integer.class)).isEqualTo(1);
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
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        createTaskInstance(1L, 3);

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
                "INSERT INTO `base_task` (id, org_id, name, description, icon, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", "Languages", "[]", 1, 1L
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
                .body("data", notNullValue())
                .body("data.nodes", hasSize(1))
                .body("data.nodes[0].baseTaskIcon", equalTo("Languages"));
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

    // ========== 业务规则：发布与模板 ==========

    /**
     * 重复公布已公布的项目应失败（覆盖 publishProjectItem PUBLISHED 分支）
     */
    @Test
    void shouldFailPublishAlreadyPublishedItem() {
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
        // status=3 → ItemStatus.PUBLISHED，已公布
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 3, 1L
        );

        Response response = authRequest(token)
                .patch("/app/projects/1/items/1/publish");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 项目列表存在数据时返回组装后的项目信息
     */
    @Test
    void shouldListItemsWithData() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem(1);
        createTaskNode(1L);
        createTaskInstance(1L, 3);

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(1))
                .body("data.list[0].title", equalTo("测试项目"))
                .body("data.list[0].progressPercent", equalTo(100));
    }

    /**
     * 存在未完成任务时不允许公布，且项目状态保持不变
     */
    @Test
    void shouldFailPublishItemWithUnfinishedTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem(1);

        // status=3 已完成 + status=1 待接取，项目仍有任务未完成
        createTaskInstance(1L, 3);
        createTaskInstance(2L, 1);

        Response response = authRequest(token)
                .patch("/app/projects/1/items/1/publish");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM item WHERE id = 1", Integer.class))
                .isEqualTo(1);
    }

    /**
     * 项目没有任何任务实例时不允许公布
     */
    @Test
    void shouldFailPublishItemWithoutTasks() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem(1);

        Response response = authRequest(token)
                .patch("/app/projects/1/items/1/publish");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM item WHERE id = 1", Integer.class))
                .isEqualTo(1);
    }

    /**
     * 只创建企划与项目本身，任务实例按用例需要补充
     */
    private void createProjectWithItem(int itemStatus) {
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
                1L, 1L, "测试项目", 0L, itemStatus, 1L
        );
    }

    private void createTaskNode(long id) {
        jdbcTemplate.update(
                "INSERT INTO `item_task_flow` (id, item_id, project_id, task_template_id, name) " +
                        "VALUES (1, 1, 1, 0, '测试流程')"
        );
        jdbcTemplate.update(
                "INSERT INTO `item_task_node` (id, item_task_flow_id, item_id, project_id, base_task_id, name, sort, parallel_sort) " +
                        "VALUES (?, 1, 1, 1, 0, '测试节点', 1, 1)",
                id
        );
    }

    private void createTaskInstance(long id, int status) {
        jdbcTemplate.update(
                "INSERT INTO `task_instance` (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, 1, 1, ?, 1, NULL, ?, NOW(), NOW())",
                id, id, status
        );
    }

    /**
     * 使用已禁用的任务模板创建项目应失败（覆盖 createProjectItem template disabled 分支）
     */
    @Test
    void shouldFailCreateItemWithDisabledTemplate() {
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
        // status=0 → DISABLED
        jdbcTemplate.update(
                "INSERT INTO `task_template` (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "禁用模板", "描述", 0, 1, 1L
        );

        Response response = authRequest(token)
                .body(Map.of("title", "测试项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items");

        response.then()
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

    /**
     * 禁用企划后，项目及任务流接口均不可访问
     */
    @Test
    void shouldRejectAllItemOperationsForDisabledProject() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, status, resource_status, creator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, "禁用企划", 1, 0, 1L);
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id) " +
                        "VALUES (?, ?, ?, ?, ?, ?)",
                1L, 1L, "测试项目", 1L, 1, 1L);

        assertProjectNotFound(authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items"));
        assertProjectNotFound(authRequest(token)
                .body(Map.of("title", "新项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items"));
        assertProjectNotFound(authRequest(token).delete("/app/projects/1/items/1"));
        assertProjectNotFound(authRequest(token).patch("/app/projects/1/items/1/publish"));
        assertProjectNotFound(authRequest(token).get("/app/item/1/flow"));
    }

    private void assertProjectNotFound(Response response) {
        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }
}

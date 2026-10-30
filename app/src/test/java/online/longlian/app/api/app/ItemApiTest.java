package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;
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
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '任务A', '[]', 1, 1), (2, 1, '启用任务', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 2, 0, 1), (2, 1, 1, 1, 1), (3, 1, 2, 2, 1)");

        authRequest(token).body(Map.of("title", "批量快照项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long itemId = jdbcTemplate.queryForObject("SELECT id FROM item WHERE title = '批量快照项目'", Long.class);
        authRequest(token).get("/app/item/" + itemId + "/flow").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes.baseTaskId", equalTo(java.util.List.of("2", "1", "2")))
                .body("data.nodes.name", equalTo(java.util.List.of("启用任务", "任务A", "启用任务")));

        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (4, 1, 999, 3, 1)");
        authRequest(token).body(Map.of("title", "缺失任务项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForList("SELECT title FROM item", String.class)).containsExactly("批量快照项目");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item_task_node", Long.class)).isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject("SELECT ref_count FROM task_template WHERE id=1", Integer.class)).isEqualTo(1);
    }

    /** 模板引用的原子任务停用后，仍可创建项目并执行该节点。 */
    @Test
    void shouldCreateAndExecuteProjectWhenTemplateReferencesDisabledBaseTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "worker", "123456", "worker@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");
        String workerToken = loginAs("worker", "123456");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '测试类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) VALUES (1, 1, 1, '测试企划', 'alias', '{}', 0, '说明', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id) VALUES (1, 1, '模板', 1, 1, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '停用后的最新任务', '[]', 0, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 1, 1, 1)");

        authRequest(token).body(Map.of("title", "停用任务项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long itemId = jdbcTemplate.queryForObject("SELECT id FROM item WHERE title = '停用任务项目'", Long.class);
        String instanceId = authRequest(token).get("/app/item/" + itemId + "/flow").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes.size()", equalTo(1))
                .body("data.nodes[0].name", equalTo("停用后的最新任务"))
                .body("data.nodes[0].taskStatus", equalTo("PENDING"))
                .extract().path("data.nodes[0].taskInstanceId");

        authRequest(workerToken, 1L).post("/app/task/instance/" + instanceId + "/claim").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(workerToken, 1L).body(Map.of("values", Map.of()))
                .post("/app/task/instance/" + instanceId + "/submit").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("提交成功"));
        authRequest(token).get("/app/item/" + itemId + "/flow").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes[0].taskStatus", equalTo("COMPLETED"));
    }


    /** 模板里已写入的外组织原子任务不能在创建项目时把名称抄进节点。 */
    @Test
    void shouldRejectCopyingAnotherOrganizationsBaseTaskNameIntoProjectNode() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织B");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '测试类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) VALUES (1, 1, 1, '测试企划', 'alias', '{}', 0, '说明', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id) VALUES (1, 1, '模板', 1, 2, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (2, 2, '组织B机密任务', '[{\"key\":\"secret\"}]', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 2, 1, 1)");

        authRequest(token).body(Map.of("title", "泄露项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForList("SELECT name FROM item_task_node", String.class))
                .doesNotContain("组织B机密任务");
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
                "INSERT INTO `item` (id, project_id, title, task_template_id, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1L
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
                .body("data.itemTitle", equalTo("测试项目"))
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


    // ========== 业务规则：完成情况筛选与模板 ==========


    /**
     * 项目列表存在数据时返回组装后的项目信息
     */
    @Test
    void shouldListItemsWithData() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem();
        createTaskNode(1L);
        createTaskInstance(1L, 3);

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .queryParam("status", "COMPLETED")
                .get("/app/projects/1/items");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(1))
                .body("data.list[0].title", equalTo("测试项目"))
                .body("data.list[0].progressPercent", equalTo(100));
    }

    @Test
    void shouldTreatItemWithoutTasksAsInProgress() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem();

        authRequest(token).queryParam("status", "IN_PROGRESS").get("/app/projects/1/items")
                .then().statusCode(200).body("code", equalTo(0)).body("data.total", equalTo(1));
        authRequest(token).queryParam("status", "COMPLETED").get("/app/projects/1/items")
                .then().statusCode(200).body("code", equalTo(0)).body("data.total", equalTo(0));
    }

    @Test
    void shouldKeepItemInProgressWhenAnyTaskIsUnfinished() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createProjectWithItem();
        createTaskInstance(1L, 3);
        createTaskInstance(2L, 1);

        authRequest(token).queryParam("status", "IN_PROGRESS").get("/app/projects/1/items")
                .then().statusCode(200).body("code", equalTo(0)).body("data.total", equalTo(1));
        authRequest(token).queryParam("status", "COMPLETED").get("/app/projects/1/items")
                .then().statusCode(200).body("code", equalTo(0)).body("data.total", equalTo(0));
    }


    /**
     * 只创建企划与项目本身，任务实例按用例需要补充
     */
    private void createProjectWithItem() {
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
                "INSERT INTO `item` (id, project_id, title, task_template_id, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1L
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
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) " +
                        "VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "测试项目", 1L, 1L);

        assertProjectNotFound(authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/app/projects/1/items"));
        assertProjectNotFound(authRequest(token)
                .body(Map.of("title", "新项目", "taskTemplateId", "1"))
                .post("/app/projects/1/items"));
        assertProjectNotFound(authRequest(token).get("/app/item/1/flow"));
    }

    /**
     * 创建时提交的步骤和执行人一次落库。组员不在组织内则整次创建失败。
     */
    @Test
    void shouldCreateDraftWithAssigneeAndRejectOutsiderWithoutPersisting() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        createTestUser(3L, "outsider", "123456", "outsider@example.com");
        String token = loginAs("creator", "123456");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 1, '企划', 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id) VALUES (1, 1, '漫画汉化', 1, 1, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '翻译', '[]', 0, 1), (2, 1, '嵌字', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 1, 1, 1)");

        authRequest(token).body(Map.of(
                "title", "第一话",
                "taskTemplateId", "1",
                "nodes", List.of(
                        Map.of("baseTaskId", "1", "sort", 1, "parallelSort", 1, "name", "翻译甲", "assigneeId", "2"),
                        Map.of("baseTaskId", "2", "sort", 2, "parallelSort", 1)
                )
        )).post("/app/projects/1/items").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Long itemId = jdbcTemplate.queryForObject("SELECT id FROM item WHERE title = '第一话'", Long.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM item_task_flow WHERE item_id = ?", String.class, itemId)).isEqualTo("漫画汉化");
        assertThat(jdbcTemplate.queryForList(
                "SELECT name FROM item_task_node WHERE item_id = ? ORDER BY sort, parallel_sort", String.class, itemId))
                .containsExactly("翻译甲", "嵌字");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM task_instance WHERE item_id = ? AND assignee_id = 2", Integer.class, itemId)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE item_id = ? AND status = 1", Long.class, itemId)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id = 1 AND user_id = 2", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT kind FROM user_notification", Integer.class)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT payload FROM outbox_message", String.class)).contains("TASK_ASSIGNED");
        assertThat(jdbcTemplate.queryForObject("SELECT ref_count FROM task_template WHERE id = 1", Integer.class)).isEqualTo(1);

        authRequest(token).body(Map.of(
                "title", "不该出现",
                "taskTemplateId", "1",
                "nodes", List.of(Map.of("baseTaskId", "2", "sort", 1, "parallelSort", 1, "assigneeId", "3"))
        )).post("/app/projects/1/items").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForList("SELECT title FROM item", String.class)).containsExactly("第一话");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_notification", Long.class)).isEqualTo(1L);
    }

    @Test
    void shouldRejectAddingDisabledTaskThatTemplateDoesNotKeep() {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("creator", "123456");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 1, '企划', 1)");
        jdbcTemplate.update("INSERT INTO task_template (id, org_id, name, status, scope, creator_id) VALUES (1, 1, '模板', 1, 1, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '翻译', '[]', 0, 1)");
        jdbcTemplate.update("INSERT INTO task_template_node (id, task_template_id, base_task_id, sort, parallel_sort) VALUES (1, 1, 1, 1, 1)");

        authRequest(token).body(Map.of(
                "title", "超额",
                "taskTemplateId", "1",
                "nodes", List.of(
                        Map.of("baseTaskId", "1", "sort", 1, "parallelSort", 1),
                        Map.of("baseTaskId", "1", "sort", 2, "parallelSort", 1)
                )
        )).post("/app/projects/1/items").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM item", Long.class)).isZero();
    }

    private void assertProjectNotFound(Response response) {
        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }
}

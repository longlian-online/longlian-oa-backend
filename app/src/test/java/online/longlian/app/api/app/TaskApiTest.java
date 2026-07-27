package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class TaskApiTest extends BaseApiTest {

    // ========== 任务模板选项 ==========

    /**
     * 获取任务模板选项列表成功
     */
    @Test
    void shouldListTaskTemplateOptionsSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO task_template (id, org_id, name, description, status, scope, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "模板1", "描述", 1, 1, 1L
        );

        Response response = authRequest(token)
                .get("/app/task-template/options");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue());
    }

    /**
     * 未认证获取任务模板选项失败
     */
    @Test
    void shouldFailListTaskTemplateOptionsWithoutAuth() {
        Response response = request()
                .get("/app/task-template/options");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务实例列表 ==========

    /**
     * 查询项目任务实例列表成功
     */
    @Test
    void shouldListTaskInstancesSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, null, 1
        );

        Response response = authRequest(token)
                .get("/app/task/instance/item/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue());
    }

    /**
     * 未认证查询任务实例列表失败
     */
    @Test
    void shouldFailListTaskInstancesWithoutAuth() {
        Response response = request()
                .get("/app/task/instance/item/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务实例详情 ==========

    /**
     * 获取任务实例详情成功
     */
    @Test
    void shouldGetTaskInstanceDetailSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, null, 1
        );

        Response response = authRequest(token)
                .get("/app/task/instance/1/detail");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue());
    }

    /**
     * 未认证获取任务实例详情失败
     */
    @Test
    void shouldFailGetTaskInstanceDetailWithoutAuth() {
        Response response = request()
                .get("/app/task/instance/1/detail");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务认领 ==========

    /**
     * 认领任务成功
     */
    @Test
    void shouldClaimTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, null, 1
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/claim");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 未认证认领任务失败
     */
    @Test
    void shouldFailClaimTaskWithoutAuth() {
        Response response = request()
                .post("/app/task/instance/1/claim");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务提交 ==========

    /**
     * 提交任务成功
     */
    @Test
    void shouldSubmitTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 2
        );

        Response response = authRequest(token)
                .body(Map.of("metadata", "{}"))
                .post("/app/task/instance/1/submit");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 未认证提交任务失败
     */
    @Test
    void shouldFailSubmitTaskWithoutAuth() {
        Response response = request()
                .body(Map.of("metadata", "{}"))
                .post("/app/task/instance/1/submit");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务放弃 ==========

    /**
     * 放弃已认领任务成功
     */
    @Test
    void shouldAbandonTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 2
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/abandon");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 未认证放弃任务失败
     */
    @Test
    void shouldFailAbandonTaskWithoutAuth() {
        Response response = request()
                .post("/app/task/instance/1/abandon");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 任务打回 ==========

    /**
     * 打回任务成功
     */
    @Test
    void shouldRejectTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "reviewer", "123456", "reviewer@example.com");
        jdbcTemplate.update("UPDATE app_user SET default_org_id = ? WHERE id = ?", 1L, 2L);
        createOrganizationMember(2L, 1L, 2L, "ORG_ADMIN");
        String token = loginAs("reviewer", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        // 打回权限要求操作人是「下一阶段执行人」：创建第二阶段节点并由 reviewer(用户2) 接取其任务实例
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                2L, 1L, 1L, 1L, 1L, "节点2", "[]", 2, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 3
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                2L, 1L, 1L, 2L, 1L, 2L, 2
        );

        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, metadata, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, "{}", 1
        );

        Response response = authRequest(token)
                .body(Map.of("reviewComment", "需要修改"))
                .post("/app/task/instance/1/reject");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 打回任务时打回意见为空应失败
     */
    @Test
    void shouldFailRejectTaskWithEmptyComment() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 3
        );

        Response response = authRequest(token)
                .body(Map.of("reviewComment", ""))
                .post("/app/task/instance/1/reject");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 打回任务时打回意见超长应失败
     */
    @Test
    void shouldFailRejectTaskWithCommentTooLong() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        String longComment = "a".repeat(501);
        Response response = authRequest(token)
                .body(Map.of("reviewComment", longComment))
                .post("/app/task/instance/1/reject");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 任务重置 ==========

    /**
     * 重置已完成任务成功
     */
    @Test
    void shouldResetTaskSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );

        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );

        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 3
        );

        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, metadata, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, "{}", 1
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/reset");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 未认证重置任务失败
     */
    @Test
    void shouldFailResetTaskWithoutAuth() {
        Response response = request()
                .post("/app/task/instance/1/reset");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 业务规则失败 ==========

    /**
     * 认领不存在的任务实例应失败
     */
    @Test
    void shouldFailClaimNonExistentTaskInstance() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .post("/app/task/instance/99999/claim");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 提交不存在的任务实例应失败
     */
    @Test
    void shouldFailSubmitNonExistentTaskInstance() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("metadata", "{}"))
                .post("/app/task/instance/99999/submit");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 放弃不存在的任务实例应失败
     */
    @Test
    void shouldFailAbandonNonExistentTaskInstance() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .post("/app/task/instance/99999/abandon");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 重置不存在的任务实例应失败
     */
    @Test
    void shouldFailResetNonExistentTaskInstance() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .post("/app/task/instance/99999/reset");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 业务规则：任务状态与权限 ==========

    /**
     * 认领已认领的任务应失败（覆盖 claim 非 PENDING 分支）
     */
    @Test
    void shouldFailClaimAlreadyClaimedTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        insertBaseTaskData();
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 2 // status=2 CLAIMED
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/claim");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 放弃 PENDING（未认领）任务应失败（覆盖 abandon 非 CLAIMED 分支）
     */
    @Test
    void shouldFailAbandonTaskWhenNotClaimed() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        insertBaseTaskData();
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, null, 1 // status=1 PENDING
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/abandon");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 非接取人放弃任务应失败（覆盖 abandon requireAssignee 分支）
     */
    @Test
    void shouldFailAbandonTaskWhenNotAssignee() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        insertBaseTaskData();
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 999L, 2 // status=2 CLAIMED，接取人为 999，非当前用户1
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/abandon");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 重置未完成（CLAIMED）的任务应失败（覆盖 reset 非 COMPLETED 分支）
     */
    @Test
    void shouldFailResetTaskWhenNotCompleted() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        insertBaseTaskData();
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 2 // status=2 CLAIMED，非 COMPLETED
        );

        Response response = authRequest(token)
                .post("/app/task/instance/1/reset");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 接取人打回自己的任务应失败（覆盖 reject userId==assigneeId 分支）
     */
    @Test
    void shouldFailRejectTaskByAssignee() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        insertBaseTaskData();
        // 当前用户(id=1)既是接取人，也是打回发起人 → 不能打回自己的任务
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, 3 // status=3 COMPLETED，接取人=1
        );
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, metadata, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, 1L, "{}", 1
        );

        Response response = authRequest(token)
                .body(Map.of("reviewComment", "需要修改"))
                .post("/app/task/instance/1/reject");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 权限检查 ==========

    /**
     * Token无效查询任务实例列表失败
     */
    @Test
    void shouldFailListTaskInstancesWithInvalidToken() {
        Response response = authRequest("invalid-token")
                .get("/app/task/instance/item/1");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // ========== 私有辅助方法 ==========

    /**
     * 插入任务实例所需的基础前置数据（project_type / project / item / base_task / flow / node）。
     * 所有 id 均为 1L，供单个测试方法复用。
     */
    private void insertBaseTaskData() {
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试类型", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, "测试企划", "alias", "{}", 0L, "描述", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "测试项目", 0L, 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon_file_id, meta_schema, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, "原子任务", "描述", null, "[]", 1, 1L
        );
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, "任务流", "描述"
        );
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 1L, 1L, 1L, "节点1", "[]", 1, 1
        );
    }
}

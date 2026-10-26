package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;

public class TaskProxySubmitApiTest extends BaseApiTest {

    private static final String SUBMITTED_AT = "2026-10-01 09:30:00";

    @Test
    void shouldProxySubmitUnclaimedTaskAsSelectedMemberAtSpecifiedTime() {
        seedPendingTask("[]");
        String token = loginAs("creator", "123456");

        Response response = authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(timestamp("task_instance", "submitted_at", 1L)).isEqualTo(SUBMITTED_AT);
        assertThat(timestamp("task_instance", "completed_at", 1L)).isEqualTo(SUBMITTED_AT);
        assertThat(timestamp("task_instance", "claimed_at", 1L)).isEqualTo(SUBMITTED_AT);
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id = 1", Long.class))
                .isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(3);
        assertThat(timestamp("task_submission", "created_at", 1L)).isEqualTo(SUBMITTED_AT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT submitter_id FROM task_submission WHERE task_instance_id = 1", Long.class)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT submit_count FROM organization_member WHERE user_id = 2", Integer.class)).isEqualTo(1);
        assertThat(timestamp("organization_member", "last_submitted_at", 2L)).isEqualTo(SUBMITTED_AT);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id = 1 AND user_id = 2 AND deleted_at IS NULL",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_workshop", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT request_body FROM user_operation_log WHERE user_id = 1", String.class))
                .contains("PROXY_SUBMIT")
                .contains(SUBMITTED_AT);

        authRequest(token).get("/app/task/instance/1/detail")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.task.status", equalTo("COMPLETED"))
                .body("data.task.assignee.id", equalTo("2"))
                .body("data.submission.submittedAt", equalTo(SUBMITTED_AT));
    }

    @Test
    void shouldKeepNewerLastSubmittedAtWhenProxySubmitIsOlder() {
        seedPendingTask("[]");
        jdbcTemplate.update(
                "UPDATE organization_member SET last_submitted_at = ?, submit_count = 4 WHERE user_id = 2",
                "2026-10-20 08:00:00");
        String token = loginAs("creator", "123456");

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(timestamp("organization_member", "last_submitted_at", 2L)).isEqualTo("2026-10-20 08:00:00");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT submit_count FROM organization_member WHERE user_id = 2", Integer.class)).isEqualTo(5);
        assertThat(timestamp("task_submission", "created_at", 1L)).isEqualTo(SUBMITTED_AT);
    }

    @Test
    void shouldRejectProxySubmitFromNonCreatorEvenIfOrgAdmin() {
        seedPendingTask("[]");
        String token = loginAs("admin", "123456");

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()))
                .body("msg", containsString("仅企划负责人可代提交"));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isZero();
    }

    @Test
    void shouldRejectProxySubmitWhenTaskIsClaimedOrCompleted() {
        seedPendingTask("[]");
        String token = loginAs("creator", "123456");
        jdbcTemplate.update("UPDATE task_instance SET status = 2, assignee_id = 2, revision = 1 WHERE id = 1");

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "1", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("该任务已接取，不能代提交"));

        jdbcTemplate.update("UPDATE task_instance SET status = 3, revision = 2 WHERE id = 1");
        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "2", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("该任务已提交，不能代提交"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isZero();
    }

    @Test
    void shouldRejectFutureTimeMissingAssigneeAndStaleRevision() {
        seedPendingTask("[]");
        String token = loginAs("creator", "123456");

        authRequest(token)
                .body(proxyBody(2L, "2099-01-01 00:00:00", "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", containsString("提交时间不能晚于当前时间"));

        authRequest(token)
                .body(Map.of("submittedAt", SUBMITTED_AT, "expectedRevision", "0", "values", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "9", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("该任务状态已变更"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldRejectDisabledOrOutsideMemberAndIncompletePreviousStage() {
        seedPendingTask("[]");
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE user_id = 4");
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) "
                        + "VALUES (2, 1, 1, 1, 1, '后一阶段', '[]', 2, 1, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, revision, created_at, updated_at) "
                        + "VALUES (2, 1, 1, 2, 1, NULL, 1, 0, NOW(), NOW())");
        String token = loginAs("creator", "123456");

        authRequest(token)
                .body(proxyBody(4L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("所选组员已被禁用"));

        authRequest(token)
                .body(proxyBody(5L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", containsString("所选组员不是当前组织成员"));

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/2/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("请先完成前置阶段"));

        authRequest(token)
                .body(proxyBody(3L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id = 1", Long.class))
                .isEqualTo(3L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM project_workshop WHERE project_id = 1 AND user_id = 3",
                Boolean.class)).isTrue();
    }

    @Test
    void shouldRejectProxySubmitWithoutRequiredField() {
        seedPendingTask("[{\"key\":\"note\",\"label\":\"备注\",\"type\":\"text\",\"required\":true,\"options\":[]}]");
        String token = loginAs("creator", "123456");

        authRequest(token)
                .body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", containsString("备注不能为空"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldListEnabledMembersOfCurrentOrganization() {
        seedPendingTask("[]");
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE user_id = 4");
        jdbcTemplate.update("UPDATE `user` SET nickname = ? WHERE id = ?", "小明", 2L);

        authRequest(loginAs("creator", "123456"))
                .get("/app/members")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.size()", equalTo(3))
                .body("data.userId", hasItems("1", "2", "3"))
                .body("data.userId", not(hasItems("4", "5")))
                .body("data.nickname", hasItems("小明"));

        authRequest(loginAs("admin", "123456"))
                .get("/app/members")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.userId", hasItems("1", "2", "3"));

        request().get("/app/members")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldFailProxySubmitWithoutAuth() {
        request().body(proxyBody(2L, SUBMITTED_AT, "0", Map.of()))
                .post("/app/task/instance/1/proxy-submit")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    private void seedPendingTask(String metaSchema) {
        createUserWithOrganization(1L, "creator", "123456", "creator@example.com", 1L, 1L, "ORG_USER");
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 2");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        createTestUser(3L, "admin", "123456", "admin@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 3");
        createOrganizationMember(3L, 1L, 3L, "ORG_ADMIN");
        createTestUser(4L, "disabled", "123456", "disabled@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 4");
        createOrganizationMember(4L, 1L, 4L, "ORG_USER");
        createTestUser(5L, "outsider", "123456", "outsider@example.com");

        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id, created_at, updated_at) VALUES (1, 1, '测试类型', 1, 1, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) "
                        + "VALUES (1, 1, 1, '测试企划', 'alias', '{}', 0, '描述', 1, 1, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id, created_at, updated_at) VALUES (1, 1, '测试项目', 0, 1, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, description, icon, meta_schema, status, creator_id, created_at, updated_at) "
                        + "VALUES (1, 1, '原子任务', '描述', NULL, '[]', 1, 1, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name, description, created_at, updated_at) "
                        + "VALUES (1, 1, 1, 1, '任务流', '描述', NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort, created_at, updated_at) "
                        + "VALUES (1, 1, 1, 1, 1, '节点1', ?, 1, 1, NOW(), NOW())",
                metaSchema);
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, revision, created_at, updated_at) "
                        + "VALUES (1, 1, 1, 1, 1, NULL, 1, 0, NOW(), NOW())");
        jdbcTemplate.update(
                "INSERT INTO project_workshop (id, project_id, user_id) VALUES (1, 1, 1), (2, 1, 2), (4, 1, 4)");
        jdbcTemplate.update(
                "INSERT INTO project_workshop (id, project_id, user_id, deleted_at) VALUES (3, 1, 3, NOW())");
    }

    private Map<String, Object> proxyBody(Long assigneeId, String submittedAt, String revision, Map<String, Object> values) {
        return Map.of(
                "assigneeId", Long.toString(assigneeId),
                "submittedAt", submittedAt,
                "expectedRevision", revision,
                "values", values);
    }

    private String timestamp(String table, String column, long id) {
        String idColumn = "organization_member".equals(table) ? "user_id" : "id";
        if ("task_submission".equals(table)) {
            return jdbcTemplate.queryForObject(
                    "SELECT DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') FROM task_submission WHERE task_instance_id = ?",
                    String.class, id);
        }
        return jdbcTemplate.queryForObject(
                "SELECT DATE_FORMAT(" + column + ", '%Y-%m-%d %H:%i:%s') FROM " + table + " WHERE " + idColumn + " = ?",
                String.class, id);
    }
}

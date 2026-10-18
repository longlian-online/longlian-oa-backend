package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class TaskSubmitStageGateApiTest extends BaseApiTest {

    @ParameterizedTest(name = "前置并行槽状态 {0} 时拒绝下一阶段提交")
    @ValueSource(ints = {1, 2})
    void shouldRejectSubmitWhilePreviousParallelSlotRemainsOpen(int openStatus) {
        String token = createWorkflow();
        insertNode(1, 1, 1, "翻译");
        insertNode(2, 1, 2, "校对");
        insertNode(3, 2, 1, "嵌字");
        insertInstance(1, 1, 1L, 3);
        insertInstance(2, 2, openStatus == 1 ? null : 9L, openStatus);
        insertInstance(3, 3, 1L, 2);

        submit(token, 3).then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,请先完成前置阶段"));

        assertStatus(3, 2);
        assertStatus(2, openStatus);
        assertSubmissionCount(3, 0);

        authRequest(token).post("/app/task/instance/3/abandon").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertStatus(3, 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE id = 3", Long.class)).isNull();
        assertStatus(2, openStatus);
        assertSubmissionCount(3, 0);
    }

    @Test
    void shouldSubmitNextStageAfterPreviousParallelSlotsComplete() {
        String token = createWorkflow();
        insertNode(1, 1, 1, "翻译");
        insertNode(2, 1, 2, "校对");
        insertNode(3, 2, 1, "嵌字");
        insertInstance(1, 1, 1L, 3);
        insertInstance(2, 2, null, 1);
        insertInstance(3, 3, 1L, 2);
        insertOtherItemPendingSlot();

        submit(token, 3).then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertStatus(3, 2);

        jdbcTemplate.update("UPDATE task_instance SET status = 3 WHERE id = 2");
        submit(token, 3).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertStatus(3, 3);
        assertStatus(2, 3);
        assertStatus(9, 1);
        assertSubmissionCount(3, 1);
    }

    @Test
    void shouldRejectSubmitWhenPreviousNodeHasNoInstance() {
        String token = createWorkflow();
        insertNode(1, 1, 1, "翻译");
        insertNode(2, 2, 1, "嵌字");
        insertInstance(2, 2, 1L, 2);

        submit(token, 2).then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,请先完成前置阶段"));
        assertStatus(2, 2);
        assertSubmissionCount(2, 0);
    }

    @Test
    void shouldSubmitCurrentStageSlotWhileSiblingIsPending() {
        String token = createWorkflow();
        insertNode(1, 1, 1, "翻译");
        insertNode(2, 1, 2, "校对");
        insertInstance(1, 1, 1L, 2);
        insertInstance(2, 2, null, 1);

        submit(token, 1).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertStatus(1, 3);
        assertStatus(2, 1);
        assertSubmissionCount(1, 1);
    }

    private String createWorkflow() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                        + "VALUES (1, 1, 1, '企划', 'alias', '{}', 0, '描述', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (1, 1, '项目', 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '原子任务', '[]', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (1, 1, 1, 1, '任务流')");
        return loginAs("owner", "123456");
    }

    private void insertOtherItemPendingSlot() {
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (2, 1, '另一项目', 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (2, 2, 1, 1, '另一任务流')");
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (9, 2, 2, 1, 1, '另一翻译', '[]', 1, 1)");
        insertInstance(9, 9, null, 1);
    }

    private void insertNode(long id, int sort, int parallelSort, String name) {
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (?, 1, 1, 1, 1, ?, '[]', ?, ?)",
                id, name, sort, parallelSort);
    }

    private void insertInstance(long id, long nodeId, Long assigneeId, int status) {
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status) "
                        + "VALUES (?, 1, ?, ?, ?, ?, ?)",
                id, nodeId == 9 ? 2L : 1L, nodeId, nodeId == 9 ? 2L : 1L, assigneeId, status);
    }

    private Response submit(String token, long instanceId) {
        return authRequest(token).body(Map.of("values", Map.of()))
                .post("/app/task/instance/" + instanceId + "/submit");
    }

    private void assertStatus(long instanceId, int status) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM task_instance WHERE id = ?", Integer.class, instanceId)).isEqualTo(status);
    }

    private void assertSubmissionCount(long instanceId, int count) {
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_submission WHERE task_instance_id = ?", Integer.class, instanceId))
                .isEqualTo(count);
    }
}

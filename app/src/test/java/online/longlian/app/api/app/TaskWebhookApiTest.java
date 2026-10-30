package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.app.impl.taskwebhook.TaskWebhookHttpClient;
import online.longlian.app.service.outbox.OutboxDispatcher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class TaskWebhookApiTest extends BaseApiTest {
    private static final String SECRET = "0123456789abcdef";

    @Autowired
    private OutboxDispatcher outboxDispatcher;
    @MockBean
    private TaskWebhookHttpClient webhookClient;

    @Test
    void shouldKeepSecretOffTheApiAndSnapshotItOntoNewNodesOnly() {
        String token = createAdmin();
        String taskId = createTask(token, "https://example.com/hooks/task", SECRET);

        authRequest(token).body(Map.of()).post("/orgadmin/task/base/list").then()
                .body("data.list[0].webhookUrl", equalTo("https://example.com/hooks/task"))
                .body("data.list[0].webhookSecretConfigured", equalTo(true))
                .body("data.list[0]", not(org.hamcrest.Matchers.hasKey("webhookSecret")));
        assertThat(secret(taskId)).isEqualTo(SECRET);

        authRequest(token).body(Map.of("name", "翻译", "submitFields", List.of(),
                        "webhookUrl", "https://example.com/hooks/task"))
                .put("/orgadmin/task/base/" + taskId).then().body("code", equalTo(0));
        assertThat(secret(taskId)).isEqualTo(SECRET);

        authRequest(token).body(Map.of("name", "翻译", "submitFields", List.of(), "webhookUrl", "http://example.com/hook"))
                .put("/orgadmin/task/base/" + taskId).then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(secret(taskId)).isEqualTo(SECRET);

        String templateId = createTemplate(token, taskId);
        seedProject();
        String oldItemId = createItem(token, templateId, "旧项目");
        assertThat(nodeUrl(oldItemId)).isEqualTo("https://example.com/hooks/task");

        authRequest(token).body(Map.of("name", "翻译", "submitFields", List.of(),
                        "webhookUrl", "https://example.com/hooks/next", "webhookSecret", "0123456789abcdef"))
                .put("/orgadmin/task/base/" + taskId).then().body("code", equalTo(0));
        String newItemId = createItem(token, templateId, "新项目");

        assertThat(nodeUrl(oldItemId)).isEqualTo("https://example.com/hooks/task");
        assertThat(nodeSecret(oldItemId)).isEqualTo(SECRET);
        assertThat(nodeUrl(newItemId)).isEqualTo("https://example.com/hooks/next");
        assertThat(nodeSecret(newItemId)).isEqualTo("0123456789abcdef");
    }

    @Test
    void shouldDeliverSubmissionAndReplayOnlyThisOrganizationsFailures() {
        String token = createAdmin();
        String taskId = createTask(token, "https://example.com/hooks/task", SECRET);
        String templateId = createTemplate(token, taskId);
        seedProject();
        String itemId = createItem(token, templateId, "第一话");
        String instanceId = authRequest(token).get("/app/item/" + itemId + "/flow")
                .jsonPath().getString("data.nodes[0].taskInstanceId");
        authRequest(token).post("/app/task/instance/" + instanceId + "/claim").then().body("code", equalTo(0));
        authRequest(token).body(Map.of("values", Map.of())).post("/app/task/instance/" + instanceId + "/submit")
                .then().body("code", equalTo(0));

        Map<String, Object> queued = jdbcTemplate.queryForMap(
                "SELECT payload, status FROM outbox_message WHERE type = 2");
        assertThat(((Number) queued.get("status")).intValue()).isEqualTo(1);
        assertThat(String.valueOf(queued.get("payload"))).contains("task.submitted");
        when(webhookClient.post(any(), any(), any(), any(), any())).thenReturn(204);

        outboxDispatcher.dispatch(testNow().plusMinutes(1));

        verify(webhookClient).post(any(), any(), any(), any(), any());
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM outbox_message WHERE type = 2", Integer.class))
                .isEqualTo(3);

        String revision = jdbcTemplate.queryForObject(
                "SELECT revision FROM task_instance WHERE id = ?", String.class, Long.valueOf(instanceId));
        authRequest(token).body(Map.of("expectedRevision", revision, "reviewComment", "重做"))
                .post("/app/task/instance/" + instanceId + "/reject").then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT payload FROM outbox_message WHERE type = 2 AND status = 1", String.class))
                .contains("task.submission_invalidated").contains("rejected");

        jdbcTemplate.update("INSERT INTO outbox_message (id, type, dedupe_key, payload, status, attempt_count, "
                        + "available_at, last_error, created_at, updated_at) VALUES (91, 2, 'foreign', "
                        + "'{\"orgId\":2,\"event\":\"task.submitted\",\"taskInstanceId\":1,\"submissionId\":1,"
                        + "\"itemTaskNodeId\":1,\"revision\":1,\"occurredAt\":\"2026-10-30 10:00:00\"}', "
                        + "4, 5, NOW(), 'webhook 响应 500', NOW(), NOW())");
        authRequest(token).post("/orgadmin/task/webhook/91/replay").then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token).body(Map.of("pageNum", 1, "pageSize", 20))
                .post("/orgadmin/task/webhook/failures").then()
                .body("data.total", equalTo(0))
                .body("data.list", empty());

        jdbcTemplate.update("UPDATE outbox_message SET status = 4, last_error = 'webhook 响应 503' WHERE type = 2 AND status = 3");
        authRequest(token).body(Map.of("pageNum", 1, "pageSize", 20))
                .post("/orgadmin/task/webhook/failures").then()
                .body("data.total", equalTo(1))
                .body("data.list[0].event", equalTo("task.submitted"))
                .body("data.list[0]", not(org.hamcrest.Matchers.hasKey("webhookUrl")));
        String failureId = authRequest(token).body(Map.of("pageNum", 1, "pageSize", 20))
                .post("/orgadmin/task/webhook/failures").jsonPath().getString("data.list[0].id");
        authRequest(token).post("/orgadmin/task/webhook/" + failureId + "/replay").then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM outbox_message WHERE id = ?", Integer.class,
                Long.valueOf(failureId))).isEqualTo(1);
    }

    private String createAdmin() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        return loginAs("orgadmin", "123456");
    }

    private String createTask(String token, String url, String secret) {
        authRequest(token).body(Map.of("name", "翻译", "submitFields", List.of(),
                        "webhookUrl", url, "webhookSecret", secret))
                .post("/orgadmin/task/base").then().body("code", equalTo(0));
        return authRequest(token).body(Map.of()).post("/orgadmin/task/base/list").jsonPath().getString("data.list[0].id");
    }

    private String createTemplate(String token, String taskId) {
        authRequest(token).body(Map.of("name", "流程", "nodes", new Object[]{
                        Map.of("baseTaskId", taskId, "sort", 1, "parallelSort", 1)}))
                .post("/orgadmin/task/template").then().body("code", equalTo(0));
        return authRequest(token).body(Map.of()).post("/orgadmin/task/template/list")
                .jsonPath().getString("data.list[0].id");
    }

    private void seedProject() {
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, creator_id) VALUES (1, 1, '类型', 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id) "
                + "VALUES (1, 1, 1, '企划', 'project', '{}', 1)");
    }

    private String createItem(String token, String templateId, String title) {
        authRequest(token).body(Map.of("title", title, "taskTemplateId", templateId))
                .post("/app/projects/1/items").then().body("code", equalTo(0));
        return authRequest(token).get("/app/projects/1/items").jsonPath()
                .getString("data.list.find { it.title == '" + title + "' }.id");
    }

    private String secret(String taskId) {
        return jdbcTemplate.queryForObject("SELECT webhook_secret FROM base_task WHERE id = ?", String.class,
                Long.valueOf(taskId));
    }

    private String nodeUrl(String itemId) {
        return jdbcTemplate.queryForObject("SELECT webhook_url FROM item_task_node WHERE item_id = ?", String.class,
                Long.valueOf(itemId));
    }

    private String nodeSecret(String itemId) {
        return jdbcTemplate.queryForObject("SELECT webhook_secret FROM item_task_node WHERE item_id = ?", String.class,
                Long.valueOf(itemId));
    }
}

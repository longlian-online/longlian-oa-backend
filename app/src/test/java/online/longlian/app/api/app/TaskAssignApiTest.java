package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.util.MailUtil;
import online.longlian.app.service.outbox.OutboxDispatcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

public class TaskAssignApiTest extends BaseApiTest {

    @Autowired
    private OutboxDispatcher outboxDispatcher;
    @MockBean
    private MailUtil mailUtil;

    private String owner;
    private String member;

    @BeforeEach
    void createFixture() {
        createUserWithOrganization(1L, "assignowner", "123456", "assignowner@example.com", 1L, 1L, "ORG_USER");
        createTestUser(2L, "assignmember", "123456", "assignmember@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 1, '分配企划', 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (1, 1, '分配项目', 0, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '翻译', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (1, 1, 1, 0, '任务流')");
        jdbcTemplate.update("INSERT INTO item_task_node "
                + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                + "VALUES (1, 1, 1, 1, 1, '翻译', '[]', 1, 1)");
        jdbcTemplate.update("INSERT INTO task_instance "
                + "(id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (1, 1, 1, 1, 1, 1)");
        owner = loginAs("assignowner", "123456");
        member = loginAs("assignmember", "123456");
    }

    /** 负责人把未接取任务分给组员后，任务直接归属该组员，并发送分配通知。 */
    @Test
    void shouldAssignPendingTaskToMemberWithoutManualClaim() {
        Response assigned = assign(owner, 2L, "0");
        assertCode(assigned, ResultCode.SUCCESS);
        assigned.then().body("data.autoJoinedProject", equalTo(true));

        assertThat(status(1L)).isEqualTo(2);
        assertThat(assignee(1L)).isEqualTo(2L);
        assertThat(jdbcTemplate.queryForObject("SELECT claimed_at FROM task_instance WHERE id=1", Object.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id=1 AND user_id=2 AND deleted_at IS NULL", Integer.class))
                .isEqualTo(1);
        authRequest(member).queryParam("bucket", "SUBMITTABLE").get("/app/task/instance/mine").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.instanceId", equalTo(List.of("1")));

        Map<String, Object> notification = jdbcTemplate.queryForMap("SELECT * FROM user_notification");
        assertThat(notification.get("recipient_user_id")).isEqualTo(2L);
        assertThat(((Number) notification.get("kind")).intValue()).isEqualTo(3);
        assertThat(notification.get("source_id")).isEqualTo(1L);
        Map<String, Object> email = jdbcTemplate.queryForMap("SELECT * FROM outbox_message");
        JSONObject payload = payloadOf(email);
        assertThat(payload.getString("kind")).isEqualTo("TASK_ASSIGNED");
        assertThat(payload.getLong("recipientUserId")).isEqualTo(2L);
        assertThat(payload.getLong("sourceId")).isEqualTo(1L);
        assertThat(String.valueOf(email.get("dedupe_key"))).isEqualTo(String.valueOf(notification.get("id")));

        authRequest(member).get("/app/notifications").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.kind", equalTo(List.of("TASK_ASSIGNED")))
                .body("data.list.sourceId", equalTo(List.of("1")))
                .body("data.list.source.taskSlot.name", equalTo(List.of("翻译")));

        outboxDispatcher.dispatch(testNow().plusSeconds(1));
        verify(mailUtil).send(argThat(mail -> "assignmember@example.com".equals(mail.receiver())
                && "longlian-oa 任务分配".equals(mail.title())
                && mail.content().contains("已由企划负责人分配给你。")
                && mail.content().contains("type=TASK_ASSIGNED")
                && mail.content().contains("taskInstanceId=1")));
    }



    /** 关闭邮件通知只跳过投递，站内信和任务归属仍保留。 */
    @Test
    void shouldKeepInboxAndSkipEmailWhenNotificationsDisabled() {
        userRequest(member).body(Map.of("config", Map.of("emailNotificationsEnabled", false)))
                .patch("/app/user/config").then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertCode(assign(owner, 2L, "0"), ResultCode.SUCCESS);
        outboxDispatcher.dispatch(testNow().plusSeconds(1));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM outbox_message", Integer.class)).isEqualTo(5);
        verify(mailUtil, never()).send(any());
        authRequest(member).get("/app/notifications").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.kind", equalTo(List.of("TASK_ASSIGNED")));
        assertThat(assignee(1L)).isEqualTo(2L);
    }

    @Test
    void shouldRejectAssignmentWithoutAuthentication() {
        request().body(body(2L, "0")).post("/app/task/instance/1/assign").then()
                .statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        assertPending();
    }

    @Test
    void shouldRejectAssignmentWithInvalidAssignee() {
        assertCode(assign(owner, 0L, "0"), ResultCode.PARAM_ERROR);
        assertPending();
    }

    @Test
    void shouldRejectAssignmentByNonCreator() {
        assertCode(assign(member, 1L, "0"), ResultCode.UNAUTHORIZED_OPERATION);
        assertPending();
        assertThat(notifications()).isZero();
    }

    @Test
    void shouldRejectAssignmentToUserOutsideOrganization() {
        createTestUser(3L, "outsider", "123456", "outsider@example.com");
        assertCode(assign(owner, 3L, "0"), ResultCode.PARAM_ERROR);
        assertPending();
        assertThat(notifications()).isZero();
    }

    @Test
    void shouldRejectAssignmentOfAlreadyClaimedTask() {
        assertCode(authRequest(member).post("/app/task/instance/1/claim"), ResultCode.SUCCESS);
        int before = notifications();
        assertCode(assign(owner, 2L, "1"), ResultCode.OPERATION_FAIL);
        assertThat(assignee(1L)).isEqualTo(2L);
        assertThat(notifications()).isEqualTo(before);
    }

    @Test
    void shouldRejectRepeatedAssignmentAfterSuccess() {
        assertCode(assign(owner, 2L, "0"), ResultCode.SUCCESS);
        int before = notifications();
        assertCode(assign(owner, 2L, "1"), ResultCode.OPERATION_FAIL);
        assertThat(status(1L)).isEqualTo(2);
        assertThat(notifications()).isEqualTo(before);
    }

    private Response assign(String token, long assigneeId, String revision) {
        return authRequest(token).body(body(assigneeId, revision)).post("/app/task/instance/1/assign");
    }

    private static Map<String, Object> body(long assigneeId, String revision) {
        return Map.of("assigneeId", assigneeId, "expectedRevision", revision);
    }

    private void assertCode(Response response, ResultCode code) {
        response.then().statusCode(200).body("code", equalTo(code.getCode()));
        if (code != ResultCode.SUCCESS) {
            response.then().body("data", nullValue());
        }
    }

    private void assertPending() {
        assertThat(status(1L)).isEqualTo(1);
        assertThat(assignee(1L)).isNull();
    }

    private int status(long id) {
        return jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=?", Integer.class, id);
    }

    private Long assignee(long id) {
        return jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id=?", Long.class, id);
    }

    private int notifications() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_notification", Integer.class);
    }

    private static JSONObject payloadOf(Map<String, Object> row) {
        Object payload = row.get("payload");
        String json = payload instanceof byte[] bytes
                ? new String(bytes, StandardCharsets.UTF_8) : String.valueOf(payload);
        return JSONObject.parseObject(json);
    }
}

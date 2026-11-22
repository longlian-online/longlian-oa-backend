package online.longlian.app.api.open;

import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 开放面写接口的端到端验证：上传资源创建与任务接取/放弃/提交的幂等语义、
 * 跨组织 NOT_FOUND、缺幂等键拒绝与状态机失败的开放信封。
 */
public class OpenWriteEndpointsApiTest extends BaseApiTest {

    private static final long USER_ID = 20001L;
    private static final long ORG_ID = 21000L;
    private static final long MEMBER_ID = 22000L;
    private static final long OTHER_USER_ID = 20002L;
    private static final long OTHER_ORG_ID = 21001L;
    private static final long OTHER_MEMBER_ID = 22001L;

    private String apiKey;

    @BeforeEach
    void createWriteFixture() {
        apiKey = createKeyForUser(USER_ID, ORG_ID, MEMBER_ID, "openwriter", "write-key");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, ?, '测试类型', 1, ?)",
                ORG_ID, USER_ID);
        jdbcTemplate.update("INSERT INTO project "
                        + "(id, org_id, type_id, title, metadata, resource_status, creator_id) "
                        + "VALUES (1, ?, 1, '开放面任务', '{}', 1, ?)",
                ORG_ID, USER_ID);
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) "
                + "VALUES (1, 1, '接取项目', 0, ?)", USER_ID);
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) "
                + "VALUES (1, ?, '接取任务', '[]', 1, ?)", ORG_ID, USER_ID);
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) "
                + "VALUES (1, 1, 1, 0, '任务流')");
        jdbcTemplate.update("INSERT INTO item_task_node "
                + "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                + "VALUES (1, 1, 1, 1, 1, '任务节点', '[]', 1, 1)");
        createTaskInstance(1L);
    }

    private String createKeyForUser(long userId, long orgId, long memberId, String username, String keyName) {
        createUserWithOrganization(userId, username, "123456", username + "@example.com", memberId, orgId, "ORG_USER");
        String token = loginAs(username, "123456");
        Response response = authRequest(token)
                .body(Map.of("name", keyName))
                .post("/app/openapi-keys");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.key", notNullValue());
        return response.jsonPath().getString("data.key");
    }

    private void createTaskInstance(long instanceId) {
        jdbcTemplate.update("INSERT INTO task_instance "
                + "(id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (?, 1, 1, 1, 1, 1)",
                instanceId);
    }

    private Response openPost(String key, String idempotencyKey, String path, Object body) {
        RequestSpecification spec = given()
                .header("X-Api-Key", key)
                .contentType(ContentType.JSON);
        if (idempotencyKey != null) {
            spec.header("X-Idempotency-Key", idempotencyKey);
        }
        if (body != null) {
            spec.body(body);
        }
        return spec.post(path);
    }

    private Map<String, Object> uploadBody(String fileName) {
        Map<String, Object> body = new HashMap<>();
        body.put("fileName", fileName);
        body.put("fileExt", "docx");
        body.put("fileSize", 100L);
        body.put("fileMime", "application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        body.put("bizType", "task_submit");
        return body;
    }

    private int instanceStatus(long instanceId) {
        return jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=?", Integer.class, instanceId);
    }

    private long instanceRevision(long instanceId) {
        return jdbcTemplate.queryForObject("SELECT revision FROM task_instance WHERE id=?", Long.class, instanceId);
    }

    private int submissionCount(long instanceId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_submission WHERE task_instance_id=?", Integer.class, instanceId);
    }

    /** task_submit 上传创建成功且字段收窄；同幂等键同请求体重发回放首次结果，不重复建资源。 */
    @Test
    void shouldCreateTaskSubmitUploadAndReplayIdempotentHit() {
        String idempotencyKey = UUID.randomUUID().toString();
        Map<String, Object> body = uploadBody("我的文件.docx");

        Response first = openPost(apiKey, idempotencyKey, "/open/v1/files", body);
        first.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.fileId", notNullValue())
                .body("data.uploadUrl", notNullValue())
                .body("data.storageType", equalTo("LOCAL"))
                // LOCAL 直传不携带 Content-Disposition，文件名还原仅对象存储链路承接（站内同口径，null 表示无需携带）
                .body("data.contentDisposition", nullValue());
        assertThat(first.body().asString()).doesNotContain("key\":");
        String fileId = first.jsonPath().getString("data.fileId");

        openPost(apiKey, idempotencyKey, "/open/v1/files", body).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.fileId", equalTo(fileId));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource", Integer.class)).isEqualTo(1);
    }

    /** 开放面业务类型只开放 task_submit，其余一律参数错误。 */
    @Test
    void shouldRejectNonTaskSubmitBizType() {
        Map<String, Object> body = uploadBody("avatar.png");
        body.put("fileExt", "png");
        body.put("fileMime", "image/png");
        body.put("bizType", "avatar");

        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/files", body).then()
                .statusCode(200)
                .body("code", equalTo(2));
    }

    /** 接取成功自动入组；同幂等键同请求体（无 body）重发回放首次结果，任务仍被同一人持有。 */
    @Test
    void shouldClaimTaskAndReplayWithoutReassigning() {
        String idempotencyKey = UUID.randomUUID().toString();

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.autoJoinedProject", equalTo(true));

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.autoJoinedProject", equalTo(true));

        assertThat(instanceStatus(1L)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE id=1", Long.class)).isEqualTo(USER_ID);
        assertThat(instanceRevision(1L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_workshop WHERE project_id=1 AND user_id=? AND deleted_at IS NULL",
                Integer.class, USER_ID)).isEqualTo(1);
    }

    /** 同幂等键不同请求体是幂等冲突，业务不重复执行。 */
    @Test
    void shouldReturnIdempotencyConflictForSameKeyDifferentBody() {
        String idempotencyKey = UUID.randomUUID().toString();

        openPost(apiKey, idempotencyKey, "/open/v1/files", uploadBody("a.docx")).then()
                .statusCode(200)
                .body("code", equalTo(0));

        openPost(apiKey, idempotencyKey, "/open/v1/files", uploadBody("b.docx")).then()
                .statusCode(200)
                .body("code", equalTo(13));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM resource", Integer.class)).isEqualTo(1);
    }

    /** 幂等键跨接口不通用：接取用过的键挪到提交是冲突，不静默放过。 */
    @Test
    void shouldConflictWhenIdempotencyKeyReusedAcrossEndpoints() {
        String idempotencyKey = UUID.randomUUID().toString();

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0));

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/submit", Map.of("values", Map.of())).then()
                .statusCode(200)
                .body("code", equalTo(13));
    }

    /** 写接口缺幂等键一律参数错误。 */
    @Test
    void shouldFailWriteWithoutIdempotencyKey() {
        openPost(apiKey, null, "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(2));
    }

    /** 跨组织访问统一 NOT_FOUND，不泄露存在性。 */
    @Test
    void shouldReturnNotFoundForCrossOrgTask() {
        String otherKey = createKeyForUser(OTHER_USER_ID, OTHER_ORG_ID, OTHER_MEMBER_ID,
                "openother", "other-key");

        openPost(otherKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(6));

        assertThat(instanceStatus(1L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE id=1", Long.class)).isNull();
    }

    /** 已被认领的任务再认领按站内状态机拒绝（站内 OPERATION_FAIL，code 5）。 */
    @Test
    void shouldRejectClaimOnAlreadyClaimedTask() {
        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0));

        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(5))
                .body("msg", notNullValue());

        assertThat(instanceStatus(1L)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE id=1", Long.class)).isEqualTo(USER_ID);
        assertThat(instanceRevision(1L)).isEqualTo(1);
    }

    /** 提交返回版本号；同幂等键重发回放首次结果，提交版本数不涨。 */
    @Test
    void shouldSubmitTaskAndReplayWithoutNewVersion() {
        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0));
        String idempotencyKey = UUID.randomUUID().toString();

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/submit", Map.of("values", Map.of())).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.versionNo", equalTo(1));

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/submit", Map.of("values", Map.of())).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.versionNo", equalTo(1));

        assertThat(instanceStatus(1L)).isEqualTo(3);
        assertThat(submissionCount(1L)).isEqualTo(1);
    }

    /** 放弃须匹配 expectedRevision；同幂等键重发不重复执行，任务回到待接取。 */
    @Test
    void shouldAbandonTaskAndReplayWithoutReexecuting() {
        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0));
        String idempotencyKey = UUID.randomUUID().toString();

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/abandon", Map.of("expectedRevision", 1)).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", nullValue());

        openPost(apiKey, idempotencyKey, "/open/v1/tasks/1/abandon", Map.of("expectedRevision", 1)).then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", nullValue());

        assertThat(instanceStatus(1L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT assignee_id FROM task_instance WHERE id=1", Long.class)).isNull();
        assertThat(instanceRevision(1L)).isEqualTo(2);
    }

    /** expectedRevision 不匹配当前修订号按站内规则拒绝（站内 OPERATION_FAIL，code 5）。 */
    @Test
    void shouldRejectAbandonWithStaleRevision() {
        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/claim", null).then()
                .statusCode(200)
                .body("code", equalTo(0));

        openPost(apiKey, UUID.randomUUID().toString(), "/open/v1/tasks/1/abandon", Map.of("expectedRevision", 0))
                .then()
                .statusCode(200)
                .body("code", equalTo(5));

        assertThat(instanceStatus(1L)).isEqualTo(2);
        assertThat(instanceRevision(1L)).isEqualTo(1);
    }
}

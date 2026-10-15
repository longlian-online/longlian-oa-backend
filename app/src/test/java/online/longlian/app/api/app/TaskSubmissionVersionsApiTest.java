package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;

@TestPropertySource(properties = {
        "storage.type=COS",
        "storage.cos.bucket=test-bucket",
        "storage.cos.region=ap-guangzhou",
        "storage.cos.secret-id=test-secret-id",
        "storage.cos.secret-key=test-secret-key",
        "storage.cdn.enabled=true",
        "storage.cdn.url-prefix=https://static.example.com",
        "storage.cdn.auth-key=test-cdn-key",
        "storage.cdn.auth-ttl-seconds=300",
        "storage.cdn.url-reuse-percent=80"
})
class TaskSubmissionVersionsApiTest extends BaseApiTest {

    @Test
    void shouldListVersionsNewestFirstWithTypedFieldsAndAttachments() {
        String token = createTask(List.of(
                field("summary", "概要", "text", false),
                field("attachment", "附件", "file", false)));
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        assertSuccess(submit(token, 1L, Map.of("summary", "v1文本", "attachment", file(101L))));
        assertSuccess(authRequest(token).post("/app/task/instance/1/reset"));
        createTaskResource(102L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        jdbcTemplate.update("UPDATE resource SET file_name = '成果102.pdf' WHERE id = 102");
        assertSuccess(submit(token, 1L, Map.of("summary", "v2文本", "attachment", file(102L))));

        Response response = versions(token, 1L);
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(2))
                .body("data[0].versionNo", equalTo(2))
                .body("data[0].status", equalTo("SUBMITTED"))
                .body("data[0].submitter.id", equalTo("1"))
                .body("data[0].submitter.nickname", equalTo("owner"))
                .body("data[0].fields.key", contains("summary", "attachment"))
                .body("data[0].fields[0].type", equalTo("text"))
                .body("data[0].fields[0].text", equalTo("v2文本"))
                .body("data[0].fields[1].type", equalTo("file"))
                .body("data[0].fields[1].file.id", equalTo("102"))
                .body("data[0].fields[1].file.name", equalTo("成果102.pdf"))
                .body("data[0].fields[1].file.availability", equalTo("available"))
                .body("data[1].versionNo", equalTo(1))
                .body("data[1].status", equalTo("RESET"))
                .body("data[1].fields[0].text", equalTo("v1文本"))
                // 历史版本资源未被替代性绑定废弃，下载地址依旧可用
                .body("data[1].fields[1].file.id", equalTo("101"))
                .body("data[1].fields[1].file.availability", equalTo("available"));

        // 重新提交后，旧版本资源保持已激活绑定，未被废弃、未被物理清理
        org.assertj.core.api.Assertions.assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 101", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());

        response.then()
                .body("data[0].fields[1].file.readUrl",
                        startsWith("https://static.example.com/task_submit/102.pdf"));
    }

    @Test
    void shouldExposeRejectedVersionReviewerAndComment() {
        String token = createTask(List.of(field("summary", "概要", "text", false)));
        assertSuccess(submit(token, 1L, Map.of("summary", "v1文本")));
        createTestUser(5L, "reviewer", "123456", "reviewer@example.com");
        jdbcTemplate.update("UPDATE task_submission SET status = 2, reviewer_id = 5, " +
                "reviewed_at = '2026-10-08 12:00:00', review_comment = '第 3 页缺图'");

        versions(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data[0].versionNo", equalTo(1))
                .body("data[0].status", equalTo("REJECTED"))
                .body("data[0].reviewer.id", equalTo("5"))
                .body("data[0].reviewer.nickname", equalTo("reviewer"))
                .body("data[0].reviewComment", equalTo("第 3 页缺图"))
                .body("data[0].reviewedAt", notNullValue());
    }

    @Test
    void shouldMarkCorruptedLegacyVersionUnreadableWithoutBlockingOthers() {
        String token = createTask(List.of(field("summary", "概要", "text", false)));
        assertSuccess(submit(token, 1L, Map.of("summary", "v1文本")));
        jdbcTemplate.update("INSERT INTO task_submission " +
                        "(id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) " +
                        "VALUES (501, 1, 1, 1, 1, 1, 2, ?, 1)", "{\"legacy\":\"value\"}");

        versions(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(2))
                .body("data[0].versionNo", equalTo(2))
                .body("data[0].unreadable", equalTo(true))
                .body("data[0].fields", empty())
                .body("data[1].versionNo", equalTo(1))
                .body("data[1].unreadable", equalTo(false))
                .body("data[1].fields[0].text", equalTo("v1文本"));
    }

    @Test
    void shouldProvideUnavailableAttachmentFallbackForLegacyFiles() {
        String token = createTask(List.of(field("attachment", "附件", "file", false)));
        jdbcTemplate.update("UPDATE task_instance SET status = 3, submitted_at = NOW() WHERE id = 1");
        Map<String, Object> values = Map.of("attachment", file(901L));
        jdbcTemplate.update("INSERT INTO task_submission " +
                        "(id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) " +
                        "VALUES (601, 1, 1, 1, 1, 1, 1, ?, 3)", JSON.toJSONString(values));

        versions(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(1))
                .body("data[0].fields[0].file.id", equalTo("901"))
                .body("data[0].fields[0].file.name", not(isEmptyOrNullString()))
                .body("data[0].fields[0].file.availability", equalTo("unavailable"))
                .body("data[0].fields[0].file.readUrl", nullValue());
    }

    @Test
    void shouldReturnEmptyVersionListForNeverSubmittedTask() {
        createTask(List.of(field("summary", "概要", "text", false)));

        versions("owner", "123456", 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", empty());
    }

    @Test
    void shouldDenyVersionsForOtherOrganizations() {
        String token = createTask(List.of(field("summary", "概要", "text", false)));
        createOrganization(2L, "其他组织");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");

        authRequest(token, 2L).get("/app/task/instance/1/submissions").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        org.junit.jupiter.api.Assertions.assertEquals(0, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_submission WHERE task_instance_id = 1", Integer.class).intValue());
    }

    @Test
    void shouldRejectVersionsForMissingInstance() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("owner", "123456");

        authRequest(token).get("/app/task/instance/1/submissions").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldRejectVersionsWhenTaskNodeMissing() {
        String token = createTask(List.of(field("summary", "概要", "text", false)));
        jdbcTemplate.update("DELETE FROM item_task_node WHERE id = 1");

        authRequest(token).get("/app/task/instance/1/submissions").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    private String createTask(List<Map<String, Object>> fields) {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project " +
                "(id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) " +
                "VALUES (1, 1, 1, '企划', 'versions', '{}', 0, '描述', 1, 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) " +
                "VALUES (1, 1, '项目', 0, 1)");
        String schema = JSON.toJSONString(fields);
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) " +
                "VALUES (1, 1, '原子任务', ?, 1, 1)", schema);
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) " +
                "VALUES (1, 1, 1, 1, '任务流')");
        jdbcTemplate.update("INSERT INTO item_task_node " +
                        "(id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) " +
                        "VALUES (1, 1, 1, 1, 1, '详情任务', ?, 1, 1)", schema);
        createTaskInstance(1L, 1L);
        return loginAs("owner", "123456");
    }

    private void createTaskInstance(long taskId, long assigneeId) {
        jdbcTemplate.update("INSERT INTO task_instance " +
                        "(id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status) " +
                        "VALUES (?, 1, 1, 1, 1, ?, 2)", taskId, assigneeId);
    }

    private void createTaskResource(long id, long orgId, long creatorId, long taskId, FileProcessStatus status) {
        jdbcTemplate.update("INSERT INTO resource " +
                        "(id, org_id, storage_type, storage_key, file_name, file_ext, file_size, file_mime, " +
                        "biz_type, biz_id, process_status, creator_id) " +
                        "VALUES (?, ?, 3, ?, ?, 'pdf', 2048, 'application/pdf', 'task_submit', ?, ?, ?)",
                id, orgId, "task_submit/" + id + ".pdf", "成果" + id + ".pdf", taskId, status.getCode(), creatorId);
    }

    private Map<String, Object> field(String key, String label, String type, boolean required) {
        return Map.of("key", key, "label", label, "type", type, "required", required, "options", List.of());
    }

    private Map<String, String> file(long id) {
        return Map.of("fileId", Long.toString(id));
    }

    private Response submit(String token, long taskId, Map<String, ?> values) {
        return authRequest(token).body(Map.of("values", values)).post("/app/task/instance/" + taskId + "/submit");
    }

    private Response versions(String token, long taskId) {
        return authRequest(token).get("/app/task/instance/" + taskId + "/submissions");
    }

    private Response versions(String username, String password, long taskId) {
        return authRequest(loginAs(username, password)).get("/app/task/instance/" + taskId + "/submissions");
    }

    private void assertSuccess(Response response) {
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }
}

package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
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
class TaskDetailApiTest extends BaseApiTest {

    @Test
    void shouldReturnIndependentTaskAndEmptySubmissionBeforeSubmit() {
        String token = createTask(List.of(field("summary", "摘要", "text", true)));

        detail(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.task.id", equalTo("1"))
                .body("data.task.name", equalTo("详情任务"))
                .body("data.task.stage", equalTo(1))
                .body("data.task.status", equalTo("CLAIMED"))
                .body("data.task.assignee.id", equalTo("1"))
                .body("data.task.assignee.nickname", equalTo("owner"))
                .body("data.submission.state", equalTo("not_submitted"))
                .body("data.submission.fields", empty())
                .body("data", not(hasKey("metadata")))
                .body("data", not(hasKey("metaSchema")));
    }

    @Test
    void shouldRenderTypedFieldsInSchemaOrderPreservingZeroAndMultilineWhitespace() {
        String token = createTask(List.of(
                field("optional", "可选文本", "text", false),
                field("count", "数量", "number", true),
                field("notes", "说明", "textarea", true),
                selectField("category", "分类", true, List.of("设计", "开发")),
                field("attachment", "附件", "file", false)));
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Activated);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'avatar', storage_key = 'avatar/101.png', "
                + "file_ext = 'png', file_mime = 'image/png' WHERE id = 101");
        jdbcTemplate.update("UPDATE user SET avatar_file_id = 101 WHERE id = 1");
        String multiline = "  第一行\n\n第二行  \n";
        Map<String, Object> values = new HashMap<>();
        values.put("category", "开发");
        values.put("notes", multiline);
        values.put("count", "0.00");
        values.put("optional", "");
        values.put("attachment", null);

        assertSuccess(submit(token, 1L, values));
        detail(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.task.status", equalTo("COMPLETED"))
                .body("data.task.assignee.id", equalTo("1"))
                .body("data.task.assignee.avatarUrl", startsWith("https://static.example.com/avatar/101.png?token="))
                .body("data.submission.state", equalTo("submitted"))
                .body("data.submission.submittedAt", notNullValue())
                .body("data.submission.fields.key", contains("optional", "count", "notes", "category", "attachment"))
                .body("data.submission.fields.label", contains("可选文本", "数量", "说明", "分类", "附件"))
                .body("data.submission.fields.type", contains("text", "text", "multiline", "text", "files"))
                .body("data.submission.fields[0].text", not(isEmptyOrNullString()))
                .body("data.submission.fields[1].text", equalTo("0"))
                .body("data.submission.fields[2].text", equalTo(multiline))
                .body("data.submission.fields[3].text", equalTo("开发"))
                .body("data.submission.fields[4].files", empty())
                .body("data.submission.fields[4].emptyText", not(isEmptyOrNullString()))
                .body("data.task", not(hasKey("metaSchema")))
                .body("data.submission", not(hasKey("metadata")));
    }

    @Test
    void shouldExposeTypedSubmitFieldsOnItemFlowWithoutRawSchema() {
        String token = createTask(List.of(field("summary", "摘要", "text", true)));

        authRequest(token).get("/app/item/1/flow").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.nodes", hasSize(1))
                .body("data.nodes[0].submitFields", hasSize(1))
                .body("data.nodes[0].submitFields[0].key", equalTo("summary"))
                .body("data.nodes[0].submitFields[0].label", equalTo("摘要"))
                .body("data.nodes[0].submitFields[0].type", equalTo("text"))
                .body("data.nodes[0].submitFields[0].required", equalTo(true))
                .body("data.nodes[0].submitFields[0].options", empty())
                .body("data.nodes[0]", not(hasKey("metaSchema")));
    }

    @Test
    void shouldRejectMissingNullAndBlankRequiredValuesWithoutCompletingTask() {
        String token = createTask(List.of(field("summary", "摘要", "text", true)));
        assertParamError(submit(token, 1L, Map.of()));
        Map<String, Object> nullValue = new HashMap<>();
        nullValue.put("summary", null);
        assertParamError(submit(token, 1L, nullValue));
        assertParamError(submit(token, 1L, Map.of("summary", " \n\t")));
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldRejectUnknownKeysAndOutOfOptionSelections() {
        String token = createTask(List.of(selectField("category", "分类", false, List.of("设计", "开发"))));
        assertParamError(submit(token, 1L, Map.of("unknown", "不能存储")));
        assertParamError(submit(token, 1L, Map.of("category", "其他")));
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldRejectNonFiniteNumbersAndNonStringScalars() {
        String token = createTask(List.of(field("count", "数量", "number", false)));
        for (Object value : List.of("NaN", "Infinity", "-Infinity", "1e9999", "不是数字", 0, false)) {
            assertParamError(submit(token, 1L, Map.of("count", value)));
        }
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldRejectClientFileMetadataAndMalformedReferences() {
        String token = createTask(List.of(field("attachment", "附件", "file", true)));
        List<Object> invalidFiles = List.of(
                "101", Map.of(), Map.of("fileId", 101), Map.of("fileId", "0"),
                Map.of("fileId", "not-an-id"), Map.of("fileId", "101", "name", "伪造名称.pdf"),
                Map.of("fileId", "101", "url", "blob:local"),
                Map.of("fileId", "101", "size", 1), List.of(Map.of("fileId", "101")));
        for (Object value : invalidFiles) {
            assertParamError(submit(token, 1L, Map.of("attachment", value)));
        }
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldRejectFileReferencesOnTextFields() {
        String token = createTask(List.of(field("summary", "摘要", "text", false)));
        assertParamError(submit(token, 1L, Map.of("summary", Map.of("fileId", "101"))));
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldDenyDetailAndSubmitAcrossOrganizations() {
        String token = createTask(List.of());
        createOrganization(2L, "其他组织");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");

        authRequest(token, 2L).get("/app/task/instance/1/detail").then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token, 2L).body(Map.of("values", Map.of()))
                .post("/app/task/instance/1/submit").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldDenySubmissionByAnotherOrganizationMember() {
        String token = createTask(List.of());
        createTestUser(2L, "other", "123456", "other@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_ADMIN");
        String otherToken = loginAs("other", "123456");

        submit(otherToken, 1L, Map.of()).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldRejectForeignUploaderOrganizationAndBusinessTypeResources() {
        String token = createTask(List.of(field("attachment", "附件", "file", true)));
        createTaskResource(101L, 1L, 2L, 0L, FileProcessStatus.Uploaded);
        createTaskResource(102L, 2L, 1L, 0L, FileProcessStatus.Uploaded);
        createTaskResource(103L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'avatar' WHERE id = 103");
        for (long id : List.of(101L, 102L, 103L)) {
            submit(token, 1L, Map.of("attachment", file(id))).then().statusCode(200)
                    .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        }
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldLeaveValidAttachmentUnboundWhenAnotherAttachmentIsInvalid() {
        String token = createTask(twoFileFields());
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        createTaskResource(102L, 1L, 2L, 0L, FileProcessStatus.Uploaded);

        submit(token, 1L, Map.of("first", file(101L), "second", file(102L))).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertUnbound(101L, FileProcessStatus.Uploaded);
        assertUnbound(102L, FileProcessStatus.Uploaded);
        assertUnsubmitted(token, 1L);

        // Successful reuse by another task proves the failed submission did not bind the valid upload.
        createTaskInstance(2L, 1L);
        assertSuccess(submit(token, 2L, Map.of("first", file(101L))));
        detail(token, 2L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.submission.fields[0].files[0].availability", equalTo("available"));
    }

    @Test
    void shouldRollbackAttachmentBindingWhenAnotherUploadIsIncomplete() {
        String token = createTask(twoFileFields());
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        createTaskResource(102L, 1L, 1L, 0L, FileProcessStatus.Pending);
        jdbcTemplate.update("UPDATE resource SET storage_type = 1, storage_key = ? WHERE id = 102",
                "task_submit/missing-" + System.nanoTime() + ".pdf");

        submit(token, 1L, Map.of("first", file(101L), "second", file(102L))).then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertUnbound(101L, FileProcessStatus.Uploaded);
        assertUnbound(102L, FileProcessStatus.Pending);
        assertUnsubmitted(token, 1L);
    }

    @Test
    void shouldReuseSameTaskAttachmentAfterResetButDenyCrossTaskReuse() {
        String token = createTask(List.of(field("attachment", "附件", "file", true)));
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        assertSuccess(submit(token, 1L, Map.of("attachment", file(101L))));
        assertSuccess(authRequest(token).post("/app/task/instance/1/reset"));
        assertUnsubmitted(token, 1L);
        assertSuccess(submit(token, 1L, Map.of("attachment", file(101L))));
        createTaskInstance(2L, 1L);

        submit(token, 2L, Map.of("attachment", file(101L))).then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertUnsubmitted(token, 2L);
        detail(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.submission.fields[0].files[0].id", equalTo("101"))
                .body("data.submission.fields[0].files[0].availability", equalTo("available"));
    }

    @Test
    void shouldReturnSignedCdnAttachmentAndActualAuthenticationExpiry() {
        String token = createTask(List.of(field("attachment", "附件", "file", true)));
        createTaskResource(101L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        assertSuccess(submit(token, 1L, Map.of("attachment", file(101L))));

        Response response = detail(token, 1L);
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.submission.fields[0].type", equalTo("files"))
                .body("data.submission.fields[0].files", hasSize(1))
                .body("data.submission.fields[0].files[0].id", equalTo("101"))
                .body("data.submission.fields[0].files[0].name", equalTo("成果101.pdf"))
                .body("data.submission.fields[0].files[0].sizeText", not(isEmptyOrNullString()))
                .body("data.submission.fields[0].files[0].mediaType", equalTo("document"))
                .body("data.submission.fields[0].files[0].availability", equalTo("available"));
        String path = "data.submission.fields[0].files[0]";
        URI uri = URI.create(response.jsonPath().getString(path + ".readUrl"));
        String timestamp = queryValue(uri.getRawQuery(), "t");
        assertThat(uri.getScheme()).isEqualTo("https");
        assertThat(uri.getHost()).isEqualTo("static.example.com");
        assertThat(uri.getRawPath()).isEqualTo("/task_submit/101.pdf");
        assertThat(queryValue(uri.getRawQuery(), "token"))
                .isEqualTo(md5("test-cdn-key" + uri.getRawPath() + timestamp));
        assertThat(response.jsonPath().getLong(path + ".expiresAt"))
                .isEqualTo(Long.parseLong(timestamp) + 300L);
        assertThat(Long.parseLong(timestamp) % 240L).isZero();
        String storedValues = jdbcTemplate.queryForObject(
                "SELECT metadata FROM task_submission WHERE task_instance_id = 1 AND status = 1", String.class);
        assertThat(JSON.parseObject(storedValues).getJSONObject("attachment"))
                .containsOnlyKeys("fileId");
    }

    @Test
    void shouldReturnUnavailableWithoutSignedUrlForMissingDeprecatedOrMismatchedAttachments() {
        List<Map<String, Object>> fields = List.of(
                field("missing", "缺失", "file", false), field("deprecated", "废弃", "file", false),
                field("otherTask", "其他任务", "file", false), field("otherOrg", "其他组织", "file", false),
                field("otherBusiness", "其他用途", "file", false), field("unbound", "未绑定", "file", false));
        String token = createTask(fields);
        createTaskResource(102L, 1L, 1L, 1L, FileProcessStatus.Deprecated);
        createTaskResource(103L, 1L, 1L, 2L, FileProcessStatus.Activated);
        createTaskResource(104L, 2L, 1L, 1L, FileProcessStatus.Activated);
        createTaskResource(105L, 1L, 1L, 1L, FileProcessStatus.Activated);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'avatar' WHERE id = 105");
        createTaskResource(106L, 1L, 1L, 0L, FileProcessStatus.Uploaded);
        Map<String, Object> values = Map.of("missing", file(101L), "deprecated", file(102L),
                "otherTask", file(103L), "otherOrg", file(104L), "otherBusiness", file(105L), "unbound", file(106L));
        jdbcTemplate.update("UPDATE task_instance SET status = 3, submitted_at = NOW() WHERE id = 1");
        jdbcTemplate.update("INSERT INTO task_submission " +
                        "(id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, metadata, status) " +
                        "VALUES (1, 1, 1, 1, 1, 1, ?, 1)", JSON.toJSONString(values));

        Response response = detail(token, 1L);
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.submission.state", equalTo("submitted"))
                .body("data.submission.fields", hasSize(6));
        for (int index = 0; index < fields.size(); index++) {
            String attachmentPath = "data.submission.fields[" + index + "].files[0]";
            response.then()
                    .body(attachmentPath + ".id", equalTo(Long.toString(101L + index)))
                    .body(attachmentPath + ".name", not(isEmptyOrNullString()))
                    .body(attachmentPath + ".availability", equalTo("unavailable"))
                    .body(attachmentPath + ".readUrl", nullValue())
                    .body(attachmentPath + ".expiresAt", nullValue());
        }
    }

    @Test
    void shouldRejectMissingTaskNodeWithoutCompletingOrRecordingSubmission() {
        String token = createTask(List.of());
        jdbcTemplate.update("DELETE FROM item_task_node WHERE id = 1");

        detail(token, 1L).then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        submit(token, 1L, Map.of()).then().statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(token).get("/app/task/instance/item/1").then().statusCode(200)
                .body("data[0].status", equalTo("CLAIMED"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM task_submission WHERE task_instance_id = 1", Integer.class)).isZero();
    }

    private String createTask(List<Map<String, Object>> fields) {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project " +
                "(id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) " +
                "VALUES (1, 1, 1, '企划', 'detail', '{}', 0, '描述', 1, 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, status, creator_id) " +
                "VALUES (1, 1, '项目', 0, 1, 1)");
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

    private Map<String, Object> selectField(String key, String label, boolean required, List<String> options) {
        return Map.of("key", key, "label", label, "type", "select", "required", required, "options", options);
    }

    private List<Map<String, Object>> twoFileFields() {
        return List.of(field("first", "第一附件", "file", false), field("second", "第二附件", "file", false));
    }

    private Map<String, String> file(long id) {
        return Map.of("fileId", Long.toString(id));
    }

    private Response submit(String token, long taskId, Map<String, ?> values) {
        return authRequest(token).body(Map.of("values", values)).post("/app/task/instance/" + taskId + "/submit");
    }

    private Response detail(String token, long taskId) {
        return authRequest(token).get("/app/task/instance/" + taskId + "/detail");
    }

    private void assertSuccess(Response response) {
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    private void assertParamError(Response response) {
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    private void assertUnsubmitted(String token, long taskId) {
        detail(token, taskId).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.task.status", equalTo("CLAIMED"))
                .body("data.submission.state", equalTo("not_submitted"))
                .body("data.submission.fields", empty());
    }

    private void assertUnbound(long resourceId, FileProcessStatus status) {
        assertThat(jdbcTemplate.queryForObject("SELECT biz_id FROM resource WHERE id = ?", Long.class, resourceId)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = ?", Integer.class, resourceId))
                .isEqualTo(status.getCode());
    }

    private String queryValue(String query, String name) {
        return Arrays.stream(query.split("&")).map(pair -> pair.split("=", 2))
                .filter(pair -> pair[0].equals(name)).map(pair -> pair[1]).findFirst().orElseThrow();
    }

    private String md5(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}

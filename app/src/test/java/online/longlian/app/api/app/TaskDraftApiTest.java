package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.constants.TaskDraftConstants;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.TestPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;

@TestPropertySource(properties = {
        "storage.type=LOCAL",
        "storage.local.directory=${java.io.tmpdir}/oa-task-draft-tests"
})
public class TaskDraftApiTest extends BaseApiTest {

    private static final String API_PATH = "/app/task/instance/1/draft";

    private String createWorkflow(String schema) {
        createUserWithOrganization(1L, "draft_owner", "123456", "draft@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update(
                "INSERT INTO project_type(id,org_id,name,status,creator_id) VALUES(1,1,'类型',1,1)");
        jdbcTemplate.update(
                "INSERT INTO"
                    + " project(id,org_id,type_id,title,alias,metadata,cover_file_id,description,status,creator_id)"
                    + " VALUES(1,1,1,'企划','alias','{}',0,'描述',1,1)");
        jdbcTemplate.update(
                "INSERT INTO item(id,project_id,title,task_template_id,creator_id)"
                    + " VALUES(1,1,'项目',0,1)");
        jdbcTemplate.update(
                "INSERT INTO base_task(id,org_id,name,meta_schema,status,creator_id)"
                    + " VALUES(1,1,'任务',?,1,1)",
                schema);
        jdbcTemplate.update(
                "INSERT INTO item_task_flow(id,item_id,project_id,task_template_id,name)"
                    + " VALUES(1,1,1,1,'流程')");
        jdbcTemplate.update(
                "INSERT INTO"
                    + " item_task_node(id,item_task_flow_id,item_id,project_id,base_task_id,name,meta_schema,sort,parallel_sort)"
                    + " VALUES(1,1,1,1,1,'任务',?,1,1)",
                schema);
        jdbcTemplate.update(
                "INSERT INTO"
                    + " task_instance(id,project_id,item_id,item_task_node_id,task_flow_id,assignee_id,status)"
                    + " VALUES(1,1,1,1,1,1,2)");
        return loginAs("draft_owner", "123456");
    }

    private Map<String, Object> createDraftSaveRequest(
            String taskRevision, String draftId, Map<String, Object> values) {
        var body = new java.util.HashMap<String, Object>();
        body.put("expectedTaskRevision", taskRevision);
        body.put("expectedDraftId", draftId);
        body.put("values", values);
        return body;
    }

    private void addParallelTask() {
        jdbcTemplate.update("INSERT INTO item_task_node(id,item_task_flow_id,item_id,project_id,base_task_id,name,meta_schema,sort,parallel_sort)"
                + " VALUES(2,1,1,1,1,'并行任务','[]',1,2)");
        jdbcTemplate.update("INSERT INTO task_instance(id,project_id,item_id,item_task_node_id,task_flow_id,status)"
                + " VALUES(2,1,1,2,1,1)");
    }

    private void saveTaskFlow(String token, String change) {
        authRequest(token).body(Map.of("nodes", List.of(
                        Map.of("id", "1", "baseTaskId", "1", "revision", "0", "sort", 1,
                                "parallelSort", change.equals("reorder") ? 2 : 1,
                                "name", change.equals("rename") ? "改名后的任务" : "任务"),
                        Map.of("id", "2", "baseTaskId", "1", "revision", "0", "sort", 1,
                                "parallelSort", change.equals("reorder") ? 1 : 2, "name", "并行任务")),
                        "removed", List.of()))
                .put("/app/item/1/flow").then().body("code", equalTo(0));
    }

    private static final String SCHEMA = "[{\"key\":\"text\",\"label\":\"说明\",\"type\":\"text\",\"required\":true,\"options\":[]},{\"key\":\"file\",\"label\":\"附件\",\"type\":\"file\",\"required\":false,\"options\":[]}]";

    private String save(String token, String taskRevision, String previousId, Map<String, Object> values) {
        var response = authRequest(token).body(createDraftSaveRequest(taskRevision, previousId, values)).put(API_PATH);
        response.then().body("code", equalTo(0));
        return response.jsonPath().getString("data.draft.id");
    }

    private Map<String, Object> submit(String taskRevision, String draftId) {
        return Map.of("expectedTaskRevision", taskRevision, "draftId", draftId);
    }

    private String upload(long id, long ownerId, long orgId) throws Exception {
        Path file = Path.of(System.getProperty("java.io.tmpdir"), "oa-task-draft-tests", "task_submit/" + id + ".txt");
        Files.createDirectories(file.getParent());
        Files.writeString(file, "original");
        jdbcTemplate.update("INSERT INTO resource(id,org_id,storage_type,storage_key,file_name,file_ext,file_size,file_mime,biz_type,biz_id,process_status,creator_id)"
                + " VALUES(?,?,1,?,'成果.txt','txt',8,'text/plain','task_submit',0,0,?)",
                id, orgId, "task_submit/" + id + ".txt", ownerId);
        return Long.toString(id);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rename", "reorder", "unchanged"})
    void shouldRetainDraftAndFilesAcrossFlowEditsAndSubmitWithoutCopying(String change) throws Exception {
        String token = createWorkflow(SCHEMA);
        addParallelTask();
        String fileId = upload(5001L, 1L, 1L);
        String id = save(token, "0", null, Map.of("text", "成果", "file", Map.of("fileId", fileId)));
        saveTaskFlow(token, change);
        authRequest(token).get(API_PATH).then().body("code", equalTo(0))
                .body("data.draft.id", equalTo(id)).body("data.draft.values.file.fileId", equalTo(fileId));
        authRequest(token).body(createDraftSaveRequest("0", id, Map.of())).put(API_PATH)
                .then().body("code", equalTo(5));
        authRequest(token).body(submit("0", id)).post(API_PATH + "/submit").then().body("code", equalTo(5));
        var response = authRequest(token).body(submit("1", id)).post(API_PATH + "/submit");
        response.then().body("code", equalTo(0));
        authRequest(token).body(submit("1", id)).post(API_PATH + "/submit").then().body("code", equalTo(0))
                .body("data.submissionId", equalTo(response.jsonPath().getString("data.submissionId")));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM task_draft WHERE id=?", String.class, id)).contains("成果", fileId);
    }

    @Test
    void shouldCreateNewSnapshotAndPreserveOldContent() {
        String token = createWorkflow(SCHEMA);
        String first = save(token, "0", null, Map.of("text", "第一稿"));
        String second = save(token, "0", first, Map.of("text", "第二稿"));
        assertThat(first).isNotEqualTo(second);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM task_draft WHERE id=?", String.class, first)).contains("第一稿");
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_draft WHERE id=?", Integer.class, first)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft WHERE status=1", Integer.class)).isEqualTo(1);
        authRequest(token).body(createDraftSaveRequest("0", first, Map.of())).put(API_PATH).then().body("code", equalTo(5));
        authRequest(token).body(submit("0", first)).post(API_PATH + "/submit").then().body("code", equalTo(5));
        authRequest(token).get(API_PATH).then().body("data.draft.id", equalTo(second));
        assertThat(jdbcTemplate.queryForObject("SELECT revision FROM task_instance WHERE id=1", Long.class)).isZero();
    }

    @Test
    void shouldAllowPartialSaveAndRequireCompleteSavedContentForSubmission() {
        String token = createWorkflow(SCHEMA);
        String first = save(token, "0", null, Map.of());
        authRequest(token).body(submit("0", first)).post(API_PATH + "/submit").then().body("code", equalTo(2));
        authRequest(token).get(API_PATH).then().body("data.draft.id", equalTo(first));
        String second = save(token, "0", first, Map.of("text", "完整成果"));
        authRequest(token).body(Map.of("values", Map.of("text", "直接提交")))
                .post("/app/task/instance/1/submit").then().body("code", equalTo(0));
        authRequest(token).body(submit("0", second)).post(API_PATH + "/submit").then().body("code", equalTo(5));
    }

    @Test
    void shouldReuseBoundFileAndRetainRemovedFileUntilAbandon() throws Exception {
        String token = createWorkflow(SCHEMA);
        String fileId = upload(5001L, 1L, 1L);
        String first = save(token, "0", null, Map.of("file", Map.of("fileId", fileId)));
        String second = save(token, "0", first, Map.of("file", Map.of("fileId", fileId), "text", "补充说明"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT biz_id FROM resource WHERE id=5001", Long.class)).isEqualTo(Long.valueOf(second));
        save(token, "0", second, Map.of("text", "移除附件"));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isEqualTo(1);
        authRequest(token).body(Map.of("expectedRevision", "0")).post("/app/task/instance/1/abandon")
                .then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT deprecated_at FROM resource WHERE id=5001", java.sql.Timestamp.class)).isNotNull();
    }

    @Test
    void shouldKeepFormalHistoryAttachmentWhenRemovedFromNewDraft() throws Exception {
        String token = createWorkflow(SCHEMA);
        String fileId = upload(5001L, 1L, 1L);
        String first = save(token, "0", null, Map.of("text", "成果", "file", Map.of("fileId", fileId)));
        authRequest(token).body(submit("0", first)).post(API_PATH + "/submit").then().body("code", equalTo(0));
        authRequest(token).post("/app/task/instance/1/reset").then().body("code", equalTo(0));
        String newFile = upload(5002L, 1L, 1L);
        String next = save(token, "2", null, Map.of("text", "修订稿", "file", Map.of("fileId", newFile)));
        save(token, "2", next, Map.of("text", "移除附件"));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isEqualTo(1);
        authRequest(token).get("/app/task/instance/1/attachments/5001/download").then().body("code", equalTo(0));
        authRequest(token).body(submit("0", first)).post(API_PATH + "/submit").then().body("code", equalTo(5));
        authRequest(token).body(Map.of("expectedRevision", "2")).post("/app/task/instance/1/abandon")
                .then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5002", Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldKeepDraftPrivateUntilItIsFormallySubmitted() throws Exception {
        String token = createWorkflow(SCHEMA);
        String fileId = upload(5001L, 1L, 1L);
        String id = save(token, "0", null, Map.of("text", "成果", "file", Map.of("fileId", fileId)));
        createTestUser(2L, "draft_viewer", "123456", "viewer@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String viewer = loginAs("draft_viewer", "123456");
        authRequest(viewer).get(API_PATH).then().body("code", not(equalTo(0)));
        authRequest(viewer).get("/app/task/instance/1/attachments/5001/download").then().body("code", not(equalTo(0)));
        authRequest(token).get(API_PATH).then().body("code", equalTo(0))
                .body("data.draft.files['5001'].availability", equalTo("available"));
        authRequest(token).get("/app/task/instance/1/attachments/5001/download").then().body("code", not(equalTo(0)));
        authRequest(token).body(submit("0", id)).post(API_PATH + "/submit").then().body("code", equalTo(0));
        authRequest(viewer).get("/app/task/instance/1/attachments/5001/download").then().body("code", equalTo(0));
    }

    @Test
    void shouldDiscardDraftOnAbandonAndNotRestoreItOnReclaim() throws Exception {
        String token = createWorkflow(SCHEMA);
        String fileId = upload(5001L, 1L, 1L);
        String id = save(token, "0", null, Map.of("file", Map.of("fileId", fileId)));
        authRequest(token).body(Map.of("expectedRevision", "0")).post("/app/task/instance/1/abandon").then().body("code", equalTo(0));
        authRequest(token).post("/app/task/instance/1/claim").then().body("code", equalTo(0));
        authRequest(token).get(API_PATH).then().body("data.draft", equalTo(null));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_draft WHERE id=?", Integer.class, id)).isEqualTo(3);
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isEqualTo(2);
        save(token, "2", null, Map.of());
    }

    @ParameterizedTest
    @ValueSource(strings = {"owner", "org", "task", "deprecated"})
    void shouldRejectForeignOrUnavailableUploadsAndRollback(String scope) throws Exception {
        String token = createWorkflow(SCHEMA);
        String fileId = upload(5001L, scope.equals("owner") ? 2L : 1L, scope.equals("org") ? 2L : 1L);
        if (scope.equals("task")) jdbcTemplate.update("UPDATE resource SET biz_id=2,process_status=1 WHERE id=5001");
        if (scope.equals("deprecated")) jdbcTemplate.update("UPDATE resource SET process_status=2 WHERE id=5001");
        authRequest(token).body(createDraftSaveRequest("0", null, Map.of("file", Map.of("fileId", fileId))))
                .put(API_PATH).then().body("code", not(equalTo(0)));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft", Integer.class)).isZero();
    }

    @Test
    void shouldRollbackNewFileBindingWhenStaleDraftIsSaved() throws Exception {
        String token = createWorkflow(SCHEMA);
        save(token, "0", null, Map.of("text", "原稿"));
        String fileId = upload(5001L, 1L, 1L);
        authRequest(token).body(createDraftSaveRequest("0", null, Map.of("file", Map.of("fileId", fileId))))
                .put(API_PATH).then().body("code", equalTo(5));
        assertThat(jdbcTemplate.queryForObject("SELECT biz_id FROM resource WHERE id=5001", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id=5001", Integer.class)).isZero();
    }

    @Test
    void shouldSerializeConcurrentFirstSaves() throws Exception {
        String token = createWorkflow("[]");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Integer> save = () -> {
                start.await();
                return authRequest(token).body(createDraftSaveRequest("0", null, Map.of())).put(API_PATH).jsonPath().getInt("code");
            };
            var first = executor.submit(save);
            var second = executor.submit(save);
            start.countDown();
            assertThat(List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS))).containsExactlyInAnyOrder(0, 5);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft WHERE status=1", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldValidateParametersAndAuthentication() {
        String token = createWorkflow("[]");
        authRequest(token).body(createDraftSaveRequest("-1", null, Map.of())).put(API_PATH).then().body("code", equalTo(2));
        authRequest(token).body(createDraftSaveRequest("0", "0", Map.of())).put(API_PATH).then().body("code", equalTo(2));
        authRequest(token).body(Map.of("expectedTaskRevision", "0")).post(API_PATH + "/submit").then().body("code", equalTo(2));
        request().get(API_PATH).then().body("code", equalTo(1));
    }

    @Test
    void shouldAllowSavingWhileWaitingButKeepDraftAfterBlockedSubmission() {
        String token = createWorkflow(SCHEMA);
        addParallelTask();
        jdbcTemplate.update("UPDATE item_task_node SET sort=2 WHERE id=1");
        String id = save(token, "0", null, Map.of("text", "等待前置的成果"));
        authRequest(token).get(API_PATH).then().body("data.canSubmit", equalTo(false));
        authRequest(token).body(submit("0", id)).post(API_PATH + "/submit").then().body("code", equalTo(5));
        authRequest(token).get(API_PATH).then().body("data.draft.id", equalTo(id));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isZero();
    }

    @Test
    void shouldSerializeConcurrentSubmissionsAndReturnOneResult() throws Exception {
        String token = createWorkflow(SCHEMA);
        String id = save(token, "0", null, Map.of("text", "成果"));
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<String> submit = () -> {
                start.await();
                var response = authRequest(token).body(submit("0", id)).post(API_PATH + "/submit");
                response.then().body("code", equalTo(0));
                return response.jsonPath().getString("data.submissionId");
            };
            var first = executor.submit(submit);
            var second = executor.submit(submit);
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS)).isEqualTo(second.get(15, TimeUnit.SECONDS));
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
    @Test
    void shouldAllowAbandoningTaskWithoutDraft() {
        String token = createWorkflow("[]");
        authRequest(token).body(Map.of("expectedRevision", "0")).post("/app/task/instance/1/abandon")
                .then().body("code", equalTo(0));
    }

    @Test
    void shouldAllowDeletingProjectWithoutDraft() {
        String token = createWorkflow("[]");
        authRequest(token).delete("/app/projects/1/items/1").then().body("code", equalTo(0));
    }

    @Test
    void shouldAllowAbandonAfterMultipleSavesSubmitAndReset() {
        String token = createWorkflow(SCHEMA);
        String first = save(token, "0", null, Map.of("text", "第一稿"));
        String second = save(token, "0", first, Map.of("text", "第二稿"));
        authRequest(token).body(submit("0", second)).post(API_PATH + "/submit").then().body("code", equalTo(0));
        authRequest(token).post("/app/task/instance/1/reset").then().body("code", equalTo(0));
        authRequest(token).body(Map.of("expectedRevision", "2")).post("/app/task/instance/1/abandon")
                .then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft WHERE status=1", Integer.class)).isZero();
    }

}

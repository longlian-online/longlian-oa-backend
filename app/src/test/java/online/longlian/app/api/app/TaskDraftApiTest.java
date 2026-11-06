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
            String taskRevision, String draftRevision, Map<String, Object> values) {
        return Map.of("expectedTaskRevision", taskRevision, "expectedDraftRevision", draftRevision, "values", values);
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

    @ParameterizedTest
    @ValueSource(strings = {"rename", "reorder", "unchanged"})
    void shouldRetainSavedDraftAndFilesAfterTaskFlowSave(String change) throws Exception {
        String schema = JSON.toJSONString(List.of(
                Map.of("key", "text", "label", "说明", "type", "text", "required", true, "options", List.of()),
                Map.of("key", "file", "label", "附件", "type", "file", "required", false, "options", List.of())));
        String token = createWorkflow(schema);
        addParallelTask();
        Path upload = Path.of(System.getProperty("java.io.tmpdir"), "oa-task-draft-tests/task_submit/5002.txt");
        Files.createDirectories(upload.getParent());
        Files.writeString(upload, "data");
        jdbcTemplate.update("INSERT INTO resource(id,org_id,storage_type,storage_key,file_name,file_ext,file_size,file_mime,biz_type,biz_id,process_status,creator_id)"
                + " VALUES(5002,1,1,'task_submit/5002.txt','成果.txt','txt',4,'text/plain','task_submit',0,0,1)");
        var saved = authRequest(token).body(createDraftSaveRequest("0", "0",
                Map.of("text", "必须保留的成果", "file", Map.of("fileId", "5002")))).put(API_PATH);
        saved.then().body("code", equalTo(0));
        String draftId = saved.jsonPath().getString("data.draft.id");
        String snapshotId = saved.jsonPath().getString("data.draft.values.file.fileId");

        saveTaskFlow(token, change);
        authRequest(token).get(API_PATH).then().body("code", equalTo(0))
                .body("data.task.revision", equalTo("1"))
                .body("data.draft.id", equalTo(draftId))
                .body("data.draftRevision", equalTo("1"))
                .body("data.draft.values.text", equalTo("必须保留的成果"))
                .body("data.draft.values.file.fileId", equalTo(snapshotId))
                .body("data.draft.files['" + snapshotId + "'].availability", equalTo("available"));
        authRequest(token).body(createDraftSaveRequest("0", "1", Map.of("text", "旧页面覆盖")))
                .put(API_PATH).then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        authRequest(token).body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                .post(API_PATH + "/submit").then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));

        // 只重新读取即可提交已保存内容，不强迫成员改动字段或再暂存一次。
        var submitRequest = Map.of("expectedTaskRevision", "1", "expectedDraftRevision", "1");
        var submitted = authRequest(token).body(submitRequest).post(API_PATH + "/submit");
        submitted.then().body("code", equalTo(0));
        String submissionId = submitted.jsonPath().getString("data.submissionId");
        authRequest(token).body(submitRequest).post(API_PATH + "/submit").then()
                .body("code", equalTo(0)).body("data.submissionId", equalTo(submissionId));
        authRequest(token).body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                .post(API_PATH + "/submit").then().body("code", not(equalTo(0)));
        assertThat(jdbcTemplate.queryForObject("SELECT task_revision FROM task_draft", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM task_submission WHERE id=?",
                String.class, Long.valueOf(submissionId))).contains("必须保留的成果", snapshotId);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission WHERE task_instance_id=1",
                Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"rename", "reorder", "unchanged"})
    void shouldAllowSavingRetainedDraftAfterTaskFlowSave(String change) {
        String token = createWorkflow("[{\"key\":\"text\",\"label\":\"说明\",\"type\":\"text\",\"required\":true,\"options\":[]}]");
        addParallelTask();
        var saved = authRequest(token).body(createDraftSaveRequest("0", "0", Map.of("text", "原有成果")))
                .put(API_PATH);
        saved.then().body("code", equalTo(0));
        String draftId = saved.jsonPath().getString("data.draft.id");

        saveTaskFlow(token, change);
        authRequest(token).get(API_PATH).then().body("code", equalTo(0))
                .body("data.draft.values.text", equalTo("原有成果"));
        authRequest(token).body(createDraftSaveRequest("1", "1", Map.of("text", "继续完善的成果")))
                .put(API_PATH).then().body("code", equalTo(0))
                .body("data.task.revision", equalTo("1"))
                .body("data.draft.id", not(equalTo(draftId)))
                .body("data.draftRevision", equalTo("2"))
                .body("data.draft.values.text", equalTo("继续完善的成果"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT task_revision FROM task_draft ORDER BY revision DESC LIMIT 1", Long.class)).isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id=1", Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldNotRestoreDraftWhenTheSameMemberReclaimsAfterTaskFlowSave() {
        String token = createWorkflow("[]");
        addParallelTask();
        authRequest(token).body(createDraftSaveRequest("0", "0", Map.of())).put(API_PATH)
                .then().body("code", equalTo(0));
        saveTaskFlow(token, "rename");
        authRequest(token).body(Map.of("expectedRevision", "1"))
                .post("/app/task/instance/1/abandon").then().body("code", equalTo(0));
        authRequest(token).post("/app/task/instance/1/claim").then().body("code", equalTo(0));
        authRequest(token).get(API_PATH).then().body("code", equalTo(0))
                .body("data.task.revision", equalTo("3"))
                .body("data.draftRevision", equalTo("2"))
                .body("data.draft", equalTo(null));
    }

    @Test
    void shouldSavePartialDraftAndSubmitOnlyTheReviewedVersion() {
        String token = createWorkflow("[{\"key\":\"text\",\"label\":\"文本\",\"type\":\"text\",\"required\":true,\"options\":[]}]");
        authRequest(token)
                .get(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.draftRevision", equalTo("0"));
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.draft.revision", equalTo("1"));
        authRequest(token)
                .body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                .post(API_PATH + "/submit")
                .then()
                .body("code", equalTo(2));
        authRequest(token)
                .body(createDraftSaveRequest("0", "1", Map.of("text", "成果")))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0));
        authRequest(token)
                .body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                .post(API_PATH + "/submit")
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo(ResultCode.OPERATION_FAIL.getMsg() + "," + TaskDraftConstants.CONFLICT_MESSAGE));
        var request = Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "2");
        authRequest(token)
                .body(request)
                .post(API_PATH + "/submit")
                .then()
                .body("code", equalTo(0))
                .body("data.status", equalTo("COMPLETED"));
        authRequest(token).body(request).post(API_PATH + "/submit").then().body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldRejectStaleSaveAndDirectSubmitAndRetainRevisionAfterAbandon() {
        String token = createWorkflow("[]");
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0));
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo(ResultCode.OPERATION_FAIL.getMsg() + "," + TaskDraftConstants.CONFLICT_MESSAGE));
        authRequest(token)
                .body(Map.of("values", Map.of()))
                .post("/app/task/instance/1/submit")
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo(ResultCode.OPERATION_FAIL.getMsg() + "," + TaskDraftConstants.CONFLICT_MESSAGE));
        authRequest(token)
                .body(Map.of("expectedRevision", "0"))
                .post("/app/task/instance/1/abandon")
                .then()
                .body("code", equalTo(0));
        authRequest(token).post("/app/task/instance/1/claim").then().body("code", equalTo(0));
        authRequest(token)
                .get(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.draftRevision", equalTo("2"));
        authRequest(token)
                .body(createDraftSaveRequest("2", "2", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.draftRevision", equalTo("3"));
    }

    @Test
    void shouldNotPermitAnotherAssigneeOrUnknownFields() {
        String token = createWorkflow("[]");
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of("unknown", "value")))
                .put(API_PATH)
                .then()
                .body("code", equalTo(2));
        jdbcTemplate.update("UPDATE task_instance SET assignee_id=999 WHERE id=1");
        authRequest(token).get(API_PATH).then().body("code", equalTo(3));
    }

    @Test
    void shouldSerializeConcurrentFirstSaves() throws Exception {
        String token = createWorkflow("[]");
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Integer> save = () -> {
                start.await();
                return authRequest(token)
                        .body(createDraftSaveRequest("0", "0", Map.of()))
                        .put(API_PATH)
                        .jsonPath()
                        .getInt("code");
            };
            var firstRequest = executor.submit(save);
            var secondRequest = executor.submit(save);
            start.countDown();
            assertThat(List.of(firstRequest.get(15, TimeUnit.SECONDS), secondRequest.get(15, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(ResultCode.SUCCESS.getCode(), ResultCode.OPERATION_FAIL.getCode());
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_draft", Integer.class))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldValidateRevisionFormatAndAuthentication() {
        String token = createWorkflow("[]");
        authRequest(token)
                .body(createDraftSaveRequest("-1", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(2));
        request().get(API_PATH).then().body("code", equalTo(1));
    }

    @Test
    void shouldFreezeFilesAndPromoteOnlyTheSavedSnapshot() throws Exception {
        String schema = JSON.toJSONString(List.of(Map.of("key", "file", "label", "附件", "type", "file",
                "required", true, "options", List.of())));
        String token = createWorkflow(schema);
        Path folder = Path.of(System.getProperty("java.io.tmpdir"), "oa-task-draft-tests");
        Path source = folder.resolve("task_submit/5001.txt");
        Files.createDirectories(source.getParent());
        Files.writeString(source, "original");
        jdbcTemplate.update(
                "INSERT INTO"
                    + " resource(id,org_id,storage_type,storage_key,file_name,file_ext,file_size,file_mime,biz_type,biz_id,process_status,creator_id)"
                    + " VALUES(5001,1,1,'task_submit/5001.txt','成果.txt','txt',8,'text/plain','task_submit',0,0,1)");
        var saved = authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of("file", Map.of("fileId", "5001"))))
                .put(API_PATH);
        saved.then().body("code", equalTo(0));
        assertThat(saved.jsonPath().getString("data.draft.values.file.fileId")).isEqualTo("5001");
        assertThat(jdbcTemplate.queryForObject("SELECT biz_type FROM resource WHERE id=5001", String.class))
                .isEqualTo("task_draft");
        authRequest(token)
                .body(createDraftSaveRequest("0", "1", Map.of("file", Map.of("fileId", "5001"))))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.draft.values.file.fileId", equalTo("5001"));
        authRequest(token)
                .body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "2"))
                .post(API_PATH + "/submit")
                .then()
                .body("code", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT biz_type FROM resource WHERE id=5001", String.class))
                .isEqualTo("task_submit");
        assertThat(jdbcTemplate.queryForObject("SELECT biz_id FROM resource WHERE id=5001", Long.class))
                .isEqualTo(1L);
        assertThat(jdbcTemplate.queryForObject("SELECT storage_key FROM resource WHERE id=5001", String.class))
                .isEqualTo("task_submit/5001.txt");
    }

    @Test
    void shouldAllowDraftWhileWaitingButNotSubmitIt() {
        String token = createWorkflow("[]");
        jdbcTemplate.update("UPDATE item_task_node SET sort=2 WHERE id=1");
        jdbcTemplate.update(
                "INSERT INTO"
                    + " item_task_node(id,item_task_flow_id,item_id,project_id,base_task_id,name,meta_schema,sort,parallel_sort)"
                    + " VALUES(2,1,1,1,1,'前置','[]',1,1)");
        jdbcTemplate.update(
                "INSERT INTO"
                    + " task_instance(id,project_id,item_id,item_task_node_id,task_flow_id,status)"
                    + " VALUES(2,1,1,2,1,1)");
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0))
                .body("data.canSubmit", equalTo(false));
        authRequest(token)
                .body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                .post(API_PATH + "/submit")
                .then()
                .body("code", equalTo(5));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_draft", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldNotExposeDraftToAnotherOrganization() {
        String token = createWorkflow("[]");
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0));
        authRequest(token, 999L)
                .get(API_PATH)
                .then()
                .body("code", not(equalTo(0)));
    }

    @Test
    void shouldMakeConcurrentSubmitIdempotent() throws Exception {
        String token = createWorkflow("[]");
        authRequest(token)
                .body(createDraftSaveRequest("0", "0", Map.of()))
                .put(API_PATH)
                .then()
                .body("code", equalTo(0));
        var executor = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        try {
            Callable<Integer> submit = () -> {
                start.await();
                return authRequest(token)
                        .body(Map.of("expectedTaskRevision", "0", "expectedDraftRevision", "1"))
                        .post(API_PATH + "/submit")
                        .jsonPath()
                        .getInt("code");
            };
            var firstRequest = executor.submit(submit);
            var secondRequest = executor.submit(submit);
            start.countDown();
            assertThat(List.of(firstRequest.get(15, TimeUnit.SECONDS), secondRequest.get(15, TimeUnit.SECONDS)))
                    .containsOnly(0);
            assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class))
                    .isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }
}

package online.longlian.app.api.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.TaskInstanceStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class TaskToolsApiTest extends BaseApiTest {

    private static final String ACCEPT = "application/json, text/event-stream";
    private final List<Session> sessions = new ArrayList<>();
    private int requestId;

    // 预接取与开始处理是不同能力，提示必须与接取接口和前置解锁规则一致。
    @Test
    void shouldAllowPreclaimButWaitToWorkUntilPredecessorSubmitted() {
        Session session = fixture();
        Map<String, String> nextTask = Map.of("projectId", "1", "itemId", "1", "instanceId", "2");
        JSONObject available = data(call(session, "get_task", nextTask)).getJSONObject("task");
        assertThat(available.getBoolean("canClaim")).isTrue();
        assertThat(available.getBoolean("canWork")).isFalse();
        assertThat(available.getJSONArray("blockedBy")).isNotEmpty();

        data(call(session, "claim_task", Map.of("instanceId", "2")));
        JSONObject claimed = data(call(session, "get_task", nextTask)).getJSONObject("task");
        assertThat(claimed.getBoolean("canClaim")).isFalse();
        assertThat(claimed.getBoolean("canWork")).isFalse();
        assertThat(data(call(session, "get_project_tasks", scope())).getJSONArray("tasks").getJSONObject(1)
                .getBoolean("canWork")).isFalse();
        assertThat(data(call(session, "list_my_tasks", Map.of("bucket", "WAITING"))).getJSONArray("list")
                .getJSONObject(0).getString("instanceId")).isEqualTo("2");

        data(call(session, "claim_task", Map.of("instanceId", "1")));
        data(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "前序成果"))));
        authRequest(session.token()).post("/app/task/instance/1/draft/submit").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(data(call(session, "get_task", nextTask)).getJSONObject("task").getBoolean("canWork")).isTrue();
        assertThat(data(call(session, "list_my_tasks", Map.of("bucket", "WAITING"))).getLong("total")).isZero();
    }

    // 非法枚举在入口返回参数名和允许值，不能暴露 SDK 的 Java 枚举转换异常。
    @ParameterizedTest
    @CsvSource({
            "list_my_tasks,bucket,bucket 必须为 SUBMITTABLE 或 WAITING",
            "list_my_tasks,sortDirection,sortDirection 必须为 DESC 或 ASC",
            "list_claimable_tasks,sortBy,sortBy 必须为 WAIT 或 LATEST 或 MOST",
            "list_claimable_tasks,stageScope,stageScope 必须为 ALL 或 CURRENT"
    })
    void shouldExplainInvalidEnumValues(String tool, String parameter, String expectedMessage) {
        Session session = fixture();
        JSONObject result = call(session, tool, Map.of(parameter, "BROKEN"));
        assertThat(result.getBoolean("isError")).isTrue();
        assertThat(result.getJSONArray("content").getJSONObject(0).getString("text"))
                .contains(expectedMessage).doesNotContain("No enum constant", "online.longlian", "java.lang");
    }

    @AfterEach
    void closeSessions() {
        for (Session session : sessions) {
            sessionRequest(session).delete("/mcp").then().statusCode(200);
        }
        sessions.clear();
    }

    // 先从广场定位任务，再接取、暂存，使用详情修订号放弃后恢复为可接取。
    @Test
    void shouldDiscoverClaimAndAbandonOwnDraftUsingCurrentRevision() {
        Session session = fixture();
        JSONObject plaza = data(call(session, "list_claimable_tasks", Map.of()));
        assertThat(plaza.getLong("total")).isEqualTo(1);
        JSONObject group = plaza.getJSONArray("list").getJSONObject(0);
        assertThat(group.get("projectId")).isEqualTo("1");
        assertThat(group.getJSONArray("tasks")).hasSize(1);
        JSONObject available = group.getJSONArray("tasks").getJSONObject(0);
        assertThat(available.get("itemId")).isEqualTo("1");
        assertThat(available.get("instanceId")).isEqualTo("1");
        assertThat(available.getBoolean("isCurrentStage")).isTrue();

        data(call(session, "claim_task", Map.of("instanceId", "1")));
        data(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "暂存"))));
        JSONObject mine = data(call(session, "list_my_tasks", Map.of()));
        assertThat(mine.getLong("total")).isEqualTo(1);
        assertThat(mine.getJSONArray("list").getJSONObject(0).get("instanceId")).isEqualTo("1");
        String revision = data(call(session, "get_task", task())).getJSONObject("task").getString("revision");
        assertThat(revision).isEqualTo("1");
        assertThat(call(session, "abandon_task", Map.of("instanceId", "1", "expectedRevision", "0"))
                .getBoolean("isError")).isTrue();
        assertThat(data(call(session, "get_task", task())).getJSONObject("draftValues").getString("summary")).isEqualTo("暂存");
        assertThat(data(call(session, "abandon_task", Map.of("instanceId", "1", "expectedRevision", revision)))
                .getBoolean("abandoned")).isTrue();
        assertThat(data(call(session, "list_my_tasks", Map.of())).getLong("total")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(TaskInstanceStatus.PENDING.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT draft_metadata FROM task_instance WHERE id = 1", String.class)).isNull();
        assertThat(data(call(session, "list_claimable_tasks", Map.of())).getJSONArray("list").getJSONObject(0)
                .getJSONArray("tasks").getJSONObject(0).get("instanceId")).isEqualTo("1");
    }

    // 广场保留阶段、关键词、任务类型筛选及企划分页；待办可切换到等前置分组。
    @Test
    void shouldFilterPlazaAndListWaitingTasks() {
        Session session = fixture();
        JSONObject all = data(call(session, "list_claimable_tasks", Map.of("stageScope", "ALL", "sortBy", "MOST", "baseTaskId", "1")));
        assertThat(all.getJSONArray("list").getJSONObject(0).getJSONArray("tasks")).hasSize(2);
        assertThat(all.getJSONArray("list").getJSONObject(0).getJSONArray("tasks").getJSONObject(1)
                .getBoolean("isCurrentStage")).isFalse();
        assertThat(data(call(session, "list_claimable_tasks", Map.of("keyword", "不存在"))).getLong("total")).isZero();
        assertThat(data(call(session, "list_claimable_tasks", Map.of("projectType", "类型", "keyword", "翻译")))
                .getJSONArray("list").getJSONObject(0).getJSONArray("tasks")).hasSize(1);
        JSONObject nextPage = data(call(session, "list_claimable_tasks", Map.of("pageNum", 2, "pageSize", 1)));
        assertThat(nextPage.getLong("total")).isEqualTo(1);
        assertThat(nextPage.getJSONArray("list")).isEmpty();
        jdbcTemplate.update("UPDATE task_instance SET status = 2, assignee_id = 1, claimed_at = NOW() WHERE id = 2");
        JSONObject waiting = data(call(session, "list_my_tasks", Map.of("bucket", "WAITING", "sortDirection", "DESC")));
        assertThat(waiting.getLong("total")).isEqualTo(1);
        assertThat(waiting.getJSONArray("list").getJSONObject(0).get("instanceId")).isEqualTo("2");
        assertThat(waiting.getJSONArray("list").getJSONObject(0).getString("waitingStageName")).isEqualTo("翻译");
        assertThat(data(call(session, "list_my_tasks", Map.of())).getJSONArray("list")).isEmpty();
    }

    // 新入口仍要求当前组织身份，分页和修订号格式错误不能改变任务。
    @Test
    void shouldRejectInvalidListAndAbandonArguments() {
        Session session = fixture();
        for (String tool : List.of("list_my_tasks", "list_claimable_tasks")) {
            assertThat(call(new Session(session.token(), session.id(), null), tool, Map.of()).getBoolean("isError")).isTrue();
            assertThat(call(new Session(session.token(), session.id(), "2"), tool, Map.of()).getBoolean("isError")).isTrue();
            assertThat(call(session, tool, Map.of("pageNum", 0)).getBoolean("isError")).isTrue();
        }
        assertThat(call(session, "list_claimable_tasks", Map.of("keyword", "x".repeat(51))).getBoolean("isError")).isTrue();
        assertThat(call(session, "abandon_task", Map.of("instanceId", "1", "expectedRevision", "-1")).getBoolean("isError")).isTrue();
        assertThat(call(session, "abandon_task", Map.of("instanceId", "1", "expectedRevision", "0")).getBoolean("isError")).isTrue();
    }

    // AI 暂存和修改不解锁后序任务；本人通过网页接口提交之后才可继续。
    @Test
    void shouldClaimDraftReviseAndContinueOnlyAfterHumanSubmission() {
        Session session = fixture();
        assertThat(data(call(session, "find_projects", Map.of("keyword", "企劃")))
                .getJSONArray("list").getJSONObject(0).getString("projectId")).isEqualTo("1");
        assertThat(data(call(session, "list_project_items", Map.of("projectId", "1")))
                .getJSONArray("list").getJSONObject(0).getString("itemId")).isEqualTo("1");
        JSONObject tasks = data(call(session, "get_project_tasks", scope()));
        assertThat(tasks.getJSONArray("tasks").getJSONObject(0).getBoolean("canClaim")).isTrue();
        assertThat(tasks.getJSONArray("tasks").getJSONObject(1).getBoolean("canClaim")).isTrue();
        assertThat(tasks.getJSONArray("tasks").getJSONObject(1).getJSONArray("blockedBy")).isNotEmpty();

        assertThat(call(session, "claim_task", Map.of("instanceId", "1")).getBoolean("isError")).isNotEqualTo(true);
        JSONObject context = data(call(session, "get_task", task()));
        assertThat(context.getJSONObject("task").getBoolean("canWork")).isTrue();
        assertThat(context.getJSONArray("submitFields").getJSONObject(0).getString("key")).isEqualTo("summary");
        assertThat(context.getString("projectDescription")).isEqualTo("请翻译为中文");

        data(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "初稿"))));
        assertThat(data(call(session, "get_task", task())).getJSONObject("draftValues").getString("summary")).isEqualTo("初稿");
        data(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "修改稿"))));
        authRequest(session.token()).get("/app/task/instance/1/draft").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.values.summary", equalTo("修改稿"));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM task_instance WHERE id = 1", Integer.class))
                .isEqualTo(TaskInstanceStatus.CLAIMED.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission", Integer.class)).isZero();
        assertThat(data(call(session, "get_project_tasks", scope())).getJSONArray("tasks").getJSONObject(1)
                .getJSONArray("blockedBy")).isNotEmpty();

        authRequest(session.token()).post("/app/task/instance/1/draft/submit").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(data(call(session, "list_my_tasks", Map.of())).getLong("total")).isZero();
        assertThat(data(call(session, "get_project_tasks", scope())).getJSONArray("tasks").getJSONObject(1)
                .getBoolean("canClaim")).isTrue();
        JSONObject next = data(call(session, "get_task", Map.of("projectId", "1", "itemId", "1", "instanceId", "2")));
        assertThat(next.getJSONArray("previousSubmissions").getJSONObject(0).getJSONObject("submission")
                .getJSONArray("fields").getJSONObject(0).getString("text")).isEqualTo("修改稿");
    }

    // 使用真实预签名地址上传原始字节，并把文件暂存到任务，不能泄露存储 key。
    @Test
    void shouldUploadFileSaveDraftAndReadItThroughAuthorizedLink() {
        Session session = fixture();
        call(session, "claim_task", Map.of("instanceId", "1"));
        byte[] bytes = "翻译成果".getBytes(StandardCharsets.UTF_8);
        JSONObject upload = data(call(session, "create_task_upload", Map.of("fileName", "译文.txt", "fileExt", "txt",
                "fileMime", "text/plain", "fileSize", bytes.length)));
        assertThat(upload).doesNotContainKeys("key", "storageType");
        assertThat(upload.getString("method")).isEqualTo("PUT");
        assertThat(upload.getJSONObject("headers")).isEmpty();
        URI target = URI.create(upload.getString("uploadUrl"));
        request().urlEncodingEnabled(false).contentType("application/octet-stream").body(bytes)
                .put(target.getRawPath() + "?" + target.getRawQuery()).then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String fileId = upload.getString("fileId");
        data(call(session, "save_task_draft", Map.of("instanceId", "1", "values",
                Map.of("summary", "正文", "attachment", Map.of("fileId", fileId)))));
        assertThat(jdbcTemplate.queryForObject("SELECT biz_type FROM resource WHERE id = ?", String.class, fileId))
                .isEqualTo("task_submit");
        JSONObject read = data(call(session, "get_task_file_url", Map.of("source", "task", "ownerId", "1", "fileId", fileId)));
        URI readTarget = URI.create(read.getString("readUrl"));
        assertThat(request().urlEncodingEnabled(false).get(readTarget.getRawPath() + "?" + readTarget.getRawQuery()).asByteArray()).isEqualTo(bytes);
        assertThat(data(call(session, "get_task", task())).getJSONObject("draftValues")
                .getJSONObject("attachment").getString("fileId")).isEqualTo(fileId);
    }

    // MCP 的每个业务工具都校验本次组织成员身份，不能借用已有会话跨组织读取。
    @Test
    void shouldRejectMissingAndUnrelatedOrganizationOnExistingSession() {
        Session session = fixture();
        assertThat(call(new Session(session.token(), session.id(), null), "find_projects", Map.of()).getBoolean("isError")).isTrue();
        assertThat(call(new Session(session.token(), session.id(), "2"), "find_projects", Map.of()).getBoolean("isError")).isTrue();
        assertThat(data(call(session, "find_projects", Map.of())).getLong("total")).isEqualTo(1);
    }

    // 管理员令牌不能作为普通用户接取或上传任务。
    @Test
    void shouldRejectAdministratorIdentityForBusinessTools() {
        createAdmin(1L, "admin", "123456", "root");
        Session session = connect(adminLoginAs("admin", "123456"), "1");
        assertThat(call(session, "claim_task", Map.of("instanceId", "1")).getBoolean("isError")).isTrue();
    }

    // 参数校验与既有任务权限原样生效，失败不能改变任务状态。
    @Test
    void shouldRejectInvalidInputAndDraftBeforeClaim() {
        Session session = fixture();
        assertThat(call(session, "find_projects", Map.of("pageSize", 0)).getBoolean("isError")).isTrue();
        assertThat(call(session, "claim_task", Map.of("instanceId", "01")).getBoolean("isError")).isTrue();
        assertThat(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "草稿")))
                .getBoolean("isError")).isTrue();
        assertThat(call(session, "create_task_upload", Map.of("fileName", "bad.exe", "fileExt", "exe", "fileMime", "text/plain", "fileSize", 1))
                .getBoolean("isError")).isTrue();
        assertThat(call(session, "get_task", Map.of("projectId", "1", "itemId", "1", "instanceId", "999"))
                .getBoolean("isError")).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource", Integer.class)).isZero();
        assertThat(data(call(session, "find_projects", Map.of("keyword", "不存在"))).getJSONArray("list")).isEmpty();
    }

    // 其他接取人的草稿不能出现在 MCP 查询中，也不能被本用户覆盖。
    @Test
    void shouldKeepAnotherAssigneesDraftPrivate() {
        Session session = fixture();
        createTestUser(2L, "other", "123456", "other@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_MEMBER");
        jdbcTemplate.update("UPDATE task_instance SET status = 2, assignee_id = 2, draft_metadata = ? WHERE id = 1",
                JSON.toJSONString(Map.of("summary", "他人的暂存")));
        JSONObject result = data(call(session, "get_task", task()));
        assertThat(result.getJSONObject("draftValues")).isEmpty();
        assertThat(result.get("draft")).isNull();
        assertThat(call(session, "save_task_draft", Map.of("instanceId", "1", "values", Map.of("summary", "覆盖")))
                .getBoolean("isError")).isTrue();
        assertThat(call(session, "abandon_task", Map.of("instanceId", "1", "expectedRevision", "0"))
                .getBoolean("isError")).isTrue();
        assertThat(jdbcTemplate.queryForObject("SELECT assignee_id FROM task_instance WHERE id = 1", Long.class)).isEqualTo(2L);
    }

    private Session fixture() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        useRequestOrg(1L);
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '类型', 1, 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                + "VALUES (1, 1, 1, '企划', 'mcp', '{}', 0, '请翻译为中文', 1, 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (1, 1, '第一话', 0, 1)");
        String fields = JSON.toJSONString(List.of(
                Map.of("key", "summary", "label", "译文", "type", "textarea", "required", true, "options", List.of()),
                Map.of("key", "attachment", "label", "文件", "type", "file", "required", false, "options", List.of())));
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '翻译', ?, 1, 1)", fields);
        jdbcTemplate.update("INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (1, 1, 1, 1, '制作流程')");
        for (int id = 1; id <= 2; id++) {
            jdbcTemplate.update("INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                    + "VALUES (?, 1, 1, 1, 1, ?, ?, ?, 1)", id, id == 1 ? "翻译" : "校对", fields, id);
            jdbcTemplate.update("INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, status) VALUES (?, 1, 1, ?, 1, 1)", id, id);
        }
        return connect(loginAs("owner", "123456"), "1");
    }

    private Session connect(String token, String orgId) {
        Response response = authRequest(token).accept(ACCEPT).body(Map.of("jsonrpc", "2.0", "id", 1, "method", "initialize",
                "params", Map.of("protocolVersion", "2025-03-26", "capabilities", Map.of(),
                        "clientInfo", Map.of("name", "task-tools-api-test", "version", "1.0")))).post("/mcp");
        envelope(response);
        Session session = new Session(token, response.header("Mcp-Session-Id"), orgId);
        sessions.add(session);
        sessionRequest(session).body(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"))
                .post("/mcp").then().statusCode(202);
        return session;
    }

    private JSONObject call(Session session, String tool, Map<String, ?> arguments) {
        return envelope(sessionRequest(session).body(Map.of("jsonrpc", "2.0", "id", ++requestId + 1, "method", "tools/call",
                "params", Map.of("name", tool, "arguments", arguments))).post("/mcp")).getJSONObject("result");
    }

    private JSONObject data(JSONObject result) {
        assertThat(result.getBoolean("isError")).as(result.toJSONString()).isNotEqualTo(true);
        return JSON.parseObject(result.getJSONArray("content").getJSONObject(0).getString("text"));
    }

    private JSONObject envelope(Response response) {
        response.then().statusCode(200);
        String body = response.asString().replace("\r\n", "\n");
        if (response.contentType().startsWith("text/event-stream")) {
            body = body.lines().filter(line -> line.startsWith("data:"))
                    .map(line -> line.substring(5).stripLeading()).collect(Collectors.joining("\n"));
        }
        JSONObject result = JSON.parseObject(body);
        assertThat(result).containsKey("result").doesNotContainKeys("code", "msg", "data", "error");
        return result;
    }

    private RequestSpecification sessionRequest(Session session) {
        RequestSpecification request = userRequest(session.token()).accept(ACCEPT)
                .header("Mcp-Session-Id", session.id()).header("MCP-Protocol-Version", "2025-03-26");
        return session.orgId() == null ? request : request.header("X-Org-Id", session.orgId());
    }

    private Map<String, String> scope() {
        return Map.of("projectId", "1", "itemId", "1");
    }

    private Map<String, String> task() {
        return Map.of("projectId", "1", "itemId", "1", "instanceId", "1");
    }

    private record Session(String token, String id, String orgId) { }
}

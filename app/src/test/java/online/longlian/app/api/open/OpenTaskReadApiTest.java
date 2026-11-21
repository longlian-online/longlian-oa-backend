package online.longlian.app.api.open;

import io.restassured.response.Response;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 开放面任务接口的端到端验证：任务流、实例详情、提交版本、附件读取链接、我的待办、任务广场。
 */
public class OpenTaskReadApiTest extends OpenApiFoundationApiTestSupport {

    private static final long USER_ID = 34001L;
    private static final long ORG_ID = 35000L;
    private static final long MEMBER_ID = 36000L;

    private static final long OTHER_USER_ID = 44001L;

    /** 提交字段快照：一个必填文本字段。 */
    private static final String META_SCHEMA =
            "[{\"key\":\"note\",\"label\":\"备注\",\"type\":\"text\",\"required\":true,\"options\":[]}]";

    private String createKey() {
        return createKeyForUser(USER_ID, "opentask", MEMBER_ID, ORG_ID);
    }

    private void seedProject(long projectId, String title, String updatedAt) {
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, 0, ?, 1, 1, ?, NOW(), ?)",
                projectId, ORG_ID, title, USER_ID, updatedAt);
    }

    private void seedItem(long itemId, long projectId, String title) {
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, sort, task_template_id, creator_id, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, 0, 0, ?, 1, NOW(), '2026-01-01 09:00:00')",
                itemId, projectId, title, USER_ID);
    }

    private void seedFlow(long flowId, long itemId, long projectId) {
        jdbcTemplate.update(
                "INSERT INTO `item_task_flow` (id, item_id, project_id, task_template_id, name, description) "
                        + "VALUES (?, ?, ?, 0, '流程', '')",
                flowId, itemId, projectId);
    }

    private void seedNode(long nodeId, long flowId, long itemId, long projectId, long baseTaskId,
                          String name, int sort, String metaSchema) {
        jdbcTemplate.update(
                "INSERT INTO `item_task_node` (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
                nodeId, flowId, itemId, projectId, baseTaskId, name, metaSchema == null ? "[]" : metaSchema, sort);
    }

    private void seedBaseTask(long baseTaskId, String name, int status) {
        jdbcTemplate.update(
                "INSERT INTO `base_task` (id, org_id, name, description, meta_schema, status, creator_id) "
                        + "VALUES (?, ?, ?, '', ?, ?, ?)",
                baseTaskId, ORG_ID, name, "[]", status, USER_ID);
    }

    private void seedInstance(long instanceId, long projectId, long itemId, long flowId, long nodeId,
                             int status, Long assigneeId, String claimedAt, long revision) {
        jdbcTemplate.update(
                "INSERT INTO `task_instance` (id, project_id, item_id, item_task_node_id, task_flow_id, "
                        + "assignee_id, claimed_at, status, revision, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, '2026-01-01 10:00:00', '2026-01-01 10:00:00')",
                instanceId, projectId, itemId, nodeId, flowId, assigneeId, claimedAt, status, revision);
    }

    private void seedSubmission(long id, long projectId, long itemId, long instanceId, long nodeId,
                               long submitterId, int versionNo, int status, String submittedAt) {
        jdbcTemplate.update(
                "INSERT INTO `task_submission` (id, project_id, item_id, task_instance_id, item_task_node_id, "
                        + "submitter_id, version_no, metadata, status, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                id, projectId, itemId, instanceId, nodeId, submitterId, versionNo,
                "{\"note\":\"第" + versionNo + "稿\"}", status, submittedAt, submittedAt);
    }

    private long seedTaskResource(long resourceId, long instanceId) {
        jdbcTemplate.update(
                "INSERT INTO `resource` (id, org_id, storage_type, storage_key, file_name, file_ext, file_size, file_mime, "
                        + "biz_type, biz_id, process_status, creator_id, created_at, updated_at) "
                        + "VALUES (?, ?, 1, ?, '成果.zip', 'zip', 4096, 'application/zip', 'task_submit', ?, 1, ?, NOW(), NOW())",
                resourceId, ORG_ID, "task_submit/" + resourceId + ".zip", instanceId, USER_ID);
        return resourceId;
    }

    /** 一套可复用的完整种子：企划 41010 + 项目 42010 + 单节点流程。 */
    private void seedSingleTaskProject(long projectId, long itemId, long flowId, long nodeId,
                                       long baseTaskId, long instanceId, String updatedAt) {
        seedProject(projectId, "任务企划" + projectId, updatedAt);
        seedItem(itemId, projectId, "任务项目" + itemId);
        seedFlow(flowId, itemId, projectId);
        seedBaseTask(baseTaskId, "写作", 1);
        seedNode(nodeId, flowId, itemId, projectId, baseTaskId, "写作节点", 0, META_SCHEMA);
        seedInstance(instanceId, projectId, itemId, flowId, nodeId, 1, null, null, 1);
    }

    @Test
    public void shouldGetItemFlowWithSubmitFieldsSnapshot() {
        String key = createKey();
        seedSingleTaskProject(41010L, 42010L, 42020L, 42030L, 43901L, 43010L, "2026-01-01 08:00:00");

        Response response = given().header("X-Api-Key", key).get("/open/v1/items/42010/flow");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.itemTitle", equalTo("任务项目42010"))
                .body("data.hasSubmissionRecord", equalTo(false))
                .body("data.nodes", hasSize(1))
                .body("data.nodes[0].baseTaskId", equalTo(43901))
                .body("data.nodes[0].name", equalTo("写作节点"))
                .body("data.nodes[0].taskInstanceId", equalTo(43010))
                .body("data.nodes[0].taskStatus", equalTo("PENDING"))
                .body("data.nodes[0].revision", equalTo("1"))
                .body("data.nodes[0].submitFields[0].key", equalTo("note"))
                .body("data.nodes[0].submitFields[0].required", equalTo(true));
        // 冻结纪律：不含图标与站内编辑标记
        org.assertj.core.api.Assertions.assertThat(response.body().asString())
                .doesNotContain("icon").doesNotContain("removable");
    }

    @Test
    public void shouldReturnNotFoundForOtherOrgItemFlow() {
        String key = createKey();
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (41011, ?, 0, '别组', 1, 1, ?, NOW(), '2026-01-01 08:00:00')",
                ORG_ID + 1, USER_ID);
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, sort, task_template_id, creator_id, status, created_at, updated_at) "
                        + "VALUES (42011, 41011, '别组项目', 0, 0, ?, 1, NOW(), '2026-01-01 09:00:00')", USER_ID);

        given().header("X-Api-Key", key).get("/open/v1/items/42011/flow").then()
                .statusCode(404)
                .body("code", equalTo(10004))
                .body("error", equalTo("OPEN_NOT_FOUND"));
    }

    @Test
    public void shouldGetTaskDetailWithSubmissionAndSlots() {
        String key = createKey();
        seedSingleTaskProject(41012L, 42012L, 42022L, 42032L, 43902L, 43012L, "2026-01-01 08:00:00");
        // 接取并提交一版
        jdbcTemplate.update("UPDATE `task_instance` SET assignee_id = ?, claimed_at = ?, status = 2 WHERE id = ?",
                USER_ID, "2026-01-02 09:00:00", 43012L);
        seedSubmission(43112L, 41012L, 42012L, 43012L, 42032L, USER_ID, 1, 1, "2026-01-02 10:00:00");

        given().header("X-Api-Key", key).get("/open/v1/tasks/43012/detail").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.taskInstanceId", equalTo(43012))
                .body("data.name", equalTo("写作节点"))
                .body("data.itemId", equalTo(42012))
                .body("data.itemTitle", equalTo("任务项目42012"))
                .body("data.projectId", equalTo(41012))
                .body("data.projectTitle", notNullValue())
                .body("data.status", equalTo("CLAIMED"))
                .body("data.revision", equalTo(1))
                .body("data.assignee.userId", equalTo((int) USER_ID))
                .body("data.assignee.nickname", notNullValue())
                .body("data.submission.state", equalTo("submitted"))
                .body("data.submission.values[0].key", equalTo("note"))
                .body("data.submission.values[0].text", equalTo("第1稿"))
                .body("data.submissionCount", equalTo(1))
                .body("data.slotIndex", equalTo(1))
                .body("data.slotTotal", equalTo(1));
    }

    @Test
    public void shouldReturnNotFoundForOtherOrgTaskDetail() {
        String key = createKey();
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, status, resource_status, creator_id, created_at, updated_at) "
                        + "VALUES (41013, ?, 0, '别组', 1, 1, ?, NOW(), '2026-01-01 08:00:00')",
                ORG_ID + 1, USER_ID);
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, sort, task_template_id, creator_id, status, created_at, updated_at) "
                        + "VALUES (42013, 41013, '别组项目', 0, 0, ?, 1, NOW(), '2026-01-01 09:00:00')", USER_ID);
        jdbcTemplate.update(
                "INSERT INTO `task_instance` (id, project_id, item_id, item_task_node_id, task_flow_id, status, revision, created_at, updated_at) "
                        + "VALUES (43013, 41013, 42013, 0, 0, 1, 1, NOW(), NOW())");

        given().header("X-Api-Key", key).get("/open/v1/tasks/43013/detail").then()
                .statusCode(404)
                .body("code", equalTo(10004));
    }

    @Test
    public void shouldListSubmissionVersionsNewestFirstWithoutAvatar() {
        String key = createKey();
        seedSingleTaskProject(41014L, 42014L, 42024L, 42034L, 43903L, 43014L, "2026-01-01 08:00:00");
        createTestUser(OTHER_USER_ID, "other_user", "123456", "other@example.com");
        seedSubmission(43114L, 41014L, 42014L, 43014L, 42034L, OTHER_USER_ID, 2, 2, "2026-01-03 10:00:00");
        seedSubmission(43115L, 41014L, 42014L, 43014L, 42034L, OTHER_USER_ID, 1, 1, "2026-01-02 10:00:00");

        Response response = given().header("X-Api-Key", key).get("/open/v1/tasks/43014/submissions");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2))
                .body("data[0].versionNo", equalTo(2))
                .body("data[0].submitter.userId", equalTo((int) OTHER_USER_ID))
                .body("data[0].submitter.nickname", notNullValue())
                .body("data[0].values[0].text", equalTo("第2稿"))
                .body("data[1].versionNo", equalTo(1));
        org.assertj.core.api.Assertions.assertThat(response.body().asString())
                .doesNotContain("avatarUrl").doesNotContain("readUrl");
    }

    @Test
    public void shouldIssueTaskAttachmentReadUrlPerCall() {
        String key = createKey();
        seedSingleTaskProject(41016L, 42016L, 42026L, 42036L, 43905L, 43016L, "2026-01-01 08:00:00");
        long fileId = seedTaskResource(40030L, 43016L);

        given().header("X-Api-Key", key)
                .get("/open/v1/tasks/43016/attachments/" + fileId + "/read-url").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.readUrl", notNullValue())
                .body("data.expireAt", notNullValue());

        // 未绑定到该任务的文件：不泄露存在性
        given().header("X-Api-Key", key)
                .get("/open/v1/tasks/43016/attachments/40099/read-url").then()
                .statusCode(404)
                .body("code", equalTo(10004));
    }

    @Test
    public void shouldPageMyTasksByClaimedAtCursor() {
        String key = createKey();
        seedProject(41017L, "待办企划一", "2026-01-01 08:00:00");
        seedProject(41018L, "待办企划二", "2026-01-02 08:00:00");
        long[] items = {42017L, 42018L};
        long[] instances = {43017L, 43018L};
        for (int i = 0; i < 2; i++) {
            seedItem(items[i], 41017L + i, "待办项目" + (i + 1));
            seedFlow(42027L + i, items[i], 41017L + i);
            seedNode(42037L + i, 42027L + i, items[i], 41017L + i, 43907L, "节点" + (i + 1), 0, META_SCHEMA);
            seedInstance(instances[i], 41017L + i, items[i], 42027L + i, 42037L + i,
                    2, USER_ID, "2026-01-0" + (5 + i) + " 09:00:00", 3 + i);
        }

        Response page1 = given().header("X-Api-Key", key)
                .queryParam("limit", 1)
                .get("/open/v1/my-tasks");
        page1.then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].taskInstanceId", equalTo(43017))
                .body("data.list[0].taskStatus", equalTo("CLAIMED"))
                .body("data.list[0].revision", equalTo(3))
                .body("data.list[0].bucket", equalTo("SUBMITTABLE"))
                .body("data.list[0].projectName", notNullValue())
                .body("data.hasMore", equalTo(true));

        String cursor = page1.jsonPath().getString("data.nextCursor");
        given().header("X-Api-Key", key)
                .queryParam("limit", 1)
                .queryParam("cursor", cursor)
                .get("/open/v1/my-tasks").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].taskInstanceId", equalTo(43018))
                .body("data.hasMore", equalTo(false))
                .body("data.nextCursor", nullValue());
    }

    @Test
    public void shouldGetTaskPlazaGroupedByProjectWithCursor() {
        String key = createKey();
        seedSingleTaskProject(41020L, 42020L, 42040L, 42050L, 43910L, 43020L, "2026-01-01 08:00:00");
        seedSingleTaskProject(41021L, 42021L, 42041L, 42051L, 43911L, 43021L, "2026-01-02 08:00:00");

        Response page1 = given().header("X-Api-Key", key)
                .queryParam("limit", 1)
                .get("/open/v1/task-plaza");
        page1.then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].projectId", equalTo(41020))
                .body("data.list[0].pendingCount", equalTo(1))
                .body("data.list[0].tasks", hasSize(1))
                .body("data.list[0].tasks[0].instanceId", equalTo(43020))
                .body("data.list[0].tasks[0].pendingSlots", equalTo(1))
                .body("data.hasMore", equalTo(true));

        String cursor = page1.jsonPath().getString("data.nextCursor");
        given().header("X-Api-Key", key)
                .queryParam("limit", 1)
                .queryParam("cursor", cursor)
                .get("/open/v1/task-plaza").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].projectId", equalTo(41021))
                .body("data.hasMore", equalTo(false));

        // 筛选：任务类型过滤掉另一企划的任务
        given().header("X-Api-Key", key)
                .queryParam("baseTaskId", 43911)
                .get("/open/v1/task-plaza").then()
                .statusCode(200)
                .body("data.list", hasSize(1))
                .body("data.list[0].projectId", equalTo(41021));

        // 筛选：关键词未命中时空列表
        given().header("X-Api-Key", key)
                .queryParam("keyword", "不存在的词")
                .get("/open/v1/task-plaza").then()
                .statusCode(200)
                .body("data.list", hasSize(0))
                .body("data.hasMore", equalTo(false));
    }

    @Test
    public void shouldListPlazaTaskTypesWithReferencedDisabled() {
        String key = createKey();
        seedBaseTask(43920L, "启用任务", 1);
        // 停用但被既有流程引用：先落停用任务，再建引用它的流程，避免同方法内主键冲突
        seedBaseTask(43921L, "停用任务", 0);
        seedBaseTask(43922L, "无关任务", 0);
        seedProject(41022L, "类型企划", "2026-01-01 08:00:00");
        seedItem(42022L, 41022L, "类型项目");
        seedFlow(42042L, 42022L, 41022L);
        seedNode(42052L, 42042L, 42022L, 41022L, 43921L, "类型节点", 0, META_SCHEMA);
        seedInstance(43022L, 41022L, 42022L, 42042L, 42052L, 1, null, null, 1);

        Response response = given().header("X-Api-Key", key).get("/open/v1/task-plaza/task-types");
        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", hasSize(2));
        String body = response.body().asString();
        org.assertj.core.api.Assertions.assertThat(body).contains("启用任务").contains("停用任务").doesNotContain("无关任务");
    }
}

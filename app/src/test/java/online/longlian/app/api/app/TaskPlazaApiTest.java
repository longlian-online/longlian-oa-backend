package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * 任务广场（缺人任务）接口测试：GET /app/task/instance/plaza
 */
public class TaskPlazaApiTest extends BaseApiTest {

    // ========== 测试数据构造 ==========

    /**
     * 标准场景：
     * - 企划A(4001, 漫画)：
     *   - 第04话(4001)：阶段1并行双槽（1已完成+1待接取），阶段2有1个待接取（未解锁，不应返回）
     *   - 第05话(4002)：阶段1有1个待接取
     * - 企划B(4002, 小说, 5天前创建)：第02卷(4003) 阶段1有1个待接取
     */
    private void seedPlazaFixture() {
        createUserWithOrganization(4001L, "plazauser", "123456", "plaza@example.com", 4001L, 4000L, "ORG_USER");
        insertProjectType(4000L, 4000L, "漫画");
        insertProjectType(4001L, 4000L, "小说");

        insertProject(4001L, 4000L, 4000L, "夏日重现汉化", 0);
        insertProject(4002L, 4000L, 4001L, "银翼杂货铺", 5);

        insertItemWithFlow(4001L, 4001L, "第04话", 4001L);
        insertItemWithFlow(4002L, 4001L, "第05话", 4002L);
        insertItemWithFlow(4003L, 4002L, "第02卷", 4003L);

        insertNode(4001L, 4001L, 4001L, 4001L, "翻译", 1, 1);
        insertNode(4002L, 4001L, 4001L, 4001L, "校对", 1, 2);
        insertNode(4003L, 4001L, 4001L, 4001L, "嵌字", 2, 1);
        insertNode(4004L, 4002L, 4002L, 4001L, "翻译", 1, 1);
        insertNode(4005L, 4003L, 4003L, 4002L, "翻译", 1, 1);

        insertInstance(4001L, 4001L, 4001L, 4001L, 3, 4001L, 0);
        insertInstance(4002L, 4001L, 4001L, 4002L, 1, null, 0);
        insertInstance(4003L, 4001L, 4001L, 4003L, 1, null, 0);
        insertInstance(4004L, 4001L, 4002L, 4004L, 1, null, 0);
        insertInstance(4005L, 4002L, 4003L, 4005L, 1, null, 5);
    }

    private void insertProjectType(long id, long orgId, String name) {
        jdbcTemplate.update(
                "INSERT INTO `project_type` (id, org_id, name, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 1, 4001, NOW(), NOW())",
                id, orgId, name);
    }

    private void insertProject(long id, long orgId, long typeId, String title, int daysAgo) {
        jdbcTemplate.update(
                "INSERT INTO `project` (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, '', '{}', 0, '', 1, 4001, ?, NOW())",
                id, orgId, typeId, title, Timestamp.valueOf(testNow().minusDays(daysAgo)));
    }

    private void insertItemWithFlow(long itemId, long projectId, String title, long flowId) {
        jdbcTemplate.update(
                "INSERT INTO `item` (id, project_id, title, task_template_id, creator_id, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 0, 4001, NOW(), NOW())",
                itemId, projectId, title);
        jdbcTemplate.update(
                "INSERT INTO `item_task_flow` (id, item_id, project_id, task_template_id, name, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 0, '任务流', NOW(), NOW())",
                flowId, itemId, projectId);
    }

    private void insertNode(long id, long flowId, long itemId, long projectId, String name, int sort, int parallelSort) {
        jdbcTemplate.update(
                "INSERT INTO `item_task_node` (id, item_task_flow_id, item_id, project_id, base_task_id, name, sort, parallel_sort, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 0, ?, ?, ?, NOW(), NOW())",
                id, flowId, itemId, projectId, name, sort, parallelSort);
    }

    private void insertInstance(long id, long projectId, long itemId, long nodeId, int status, Long assigneeId, int daysAgo) {
        jdbcTemplate.update(
                "INSERT INTO `task_instance` (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, 0, ?, ?, ?, NOW())",
                id, projectId, itemId, nodeId, assigneeId, status, Timestamp.valueOf(testNow().minusDays(daysAgo)));
    }

    @Test
    void shouldListAllStagesWithAccurateStageMarkers() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");
        authRequest(token).queryParam("stageScope", "ALL")
                .get("/app/task/instance/plaza").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[1].pendingCount", equalTo(3))
                .body("data.list[1].tasks.size()", equalTo(3))
                .body("data.list[1].tasks[0].instanceId", equalTo("4002"))
                .body("data.list[1].tasks[0].isCurrentStage", equalTo(true))
                .body("data.list[1].tasks[1].instanceId", equalTo("4003"))
                .body("data.list[1].tasks[1].isCurrentStage", equalTo(false))
                .body("data.list[1].tasks[1].stageIndex", equalTo(2));
        authRequest(token).queryParam("stageScope", "CURRENT")
                .get("/app/task/instance/plaza").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[1].pendingCount", equalTo(2))
                .body("data.list[1].tasks.size()", equalTo(2))
                .body("data.list[1].tasks[0].isCurrentStage", equalTo(true));
        authRequest(token).post("/app/task/instance/4003/claim").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).queryParam("stageScope", "ALL")
                .get("/app/task/instance/plaza").then()
                .body("data.list[1].pendingCount", equalTo(2));
    }

    @Test
    void shouldRejectUnknownStageScope() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");
        authRequest(token).queryParam("stageScope", "INVALID")
                .get("/app/task/instance/plaza").then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldResizePlazaThumbnailsAndLeaveCoversOriginal() {
        seedPlazaFixture();
        createResource(4101L, 4000L, 4001L);
        createResource(4102L, 4000L, 4001L);
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = 'thumbnail', storage_key = 'thumbnail/4101.png' WHERE id = 4101");
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = 'cover', storage_key = 'cover/4102.png' WHERE id = 4102");
        jdbcTemplate.update(
                "UPDATE project SET thumbnail_file_id = 4101, cover_file_id = 4102 WHERE id = 4001");
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token).get("/app/task/instance/plaza");

        response.then().statusCode(200).body("code", equalTo(0));
        String thumbnailUrl = response.path("data.list.find { it.projectId == '4001' }.thumbnailUrl");
        String coverUrl = response.path("data.list.find { it.projectId == '4001' }.coverUrl");
        Object absentThumbnail = response.path("data.list.find { it.projectId == '4002' }.thumbnailUrl");
        assertThat(thumbnailUrl).contains("key=thumbnail/4101.png")
                .contains("&eo-img.resize=w/144/h/144");
        assertThat(coverUrl).contains("key=cover/4102.png").doesNotContain("eo-img.resize");
        assertThat(absentThumbnail).isNull();
    }

    // ========== 成功路径 ==========

    @Test
    void shouldListPlazaGroupsSortedByWaitSuccessfully() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token).get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(2))
                .body("data.list.size()", equalTo(2))
                // 等待最久的企划B排第一
                .body("data.list[0].projectId", equalTo("4002"))
                .body("data.list[0].title", equalTo("银翼杂货铺"))
                .body("data.list[0].typeName", equalTo("小说"))
                .body("data.list[0].pendingCount", equalTo(1))
                .body("data.list[0].itemCount", equalTo(1))
                .body("data.list[0].tasks.size()", equalTo(1))
                .body("data.list[0].tasks[0].instanceId", equalTo("4005"))
                .body("data.list[0].tasks[0].stageIndex", equalTo(1))
                .body("data.list[0].tasks[0].stageTotal", equalTo(1))
                .body("data.list[0].tasks[0].parallelTotal", equalTo(1))
                .body("data.list[0].tasks[0].slotIndex", equalTo(1))
                .body("data.list[0].tasks[0].slotTotal", equalTo(1))
                .body("data.list[0].tasks[0].serialStep", nullValue())
                .body("data.list[0].tasks[0].filledSlots", equalTo(0))
                .body("data.list[0].tasks[0].pendingSlots", equalTo(1))
                .body("data.list[0].tasks[0].createdAt", notNullValue())
                // 企划A排第二，任务按条目与阶段排序
                .body("data.list[1].projectId", equalTo("4001"))
                .body("data.list[1].pendingCount", equalTo(2))
                .body("data.list[1].itemCount", equalTo(2))
                .body("data.list[1].tasks.size()", equalTo(2))
                .body("data.list[1].tasks[0].instanceId", equalTo("4002"))
                .body("data.list[1].tasks[0].nodeName", equalTo("校对"))
                .body("data.list[1].tasks[0].itemTitle", equalTo("第04话"))
                .body("data.list[1].tasks[0].stageIndex", equalTo(1))
                .body("data.list[1].tasks[0].stageTotal", equalTo(2))
                .body("data.list[1].tasks[0].parallelTotal", equalTo(2))
                .body("data.list[1].tasks[0].slotIndex", equalTo(2))
                .body("data.list[1].tasks[0].slotTotal", equalTo(2))
                .body("data.list[1].tasks[0].serialStep", nullValue())
                .body("data.list[1].tasks[0].filledSlots", equalTo(1))
                .body("data.list[1].tasks[0].pendingSlots", equalTo(1))
                .body("data.list[1].tasks[1].instanceId", equalTo("4004"))
                .body("data.list[1].tasks[1].itemTitle", equalTo("第05话"))
                .body("data.list[1].tasks[1].stageTotal", equalTo(1));
    }

    @Test
    void shouldSortGroupsByLatestPublish() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("sortBy", "LATEST")
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                // 最新发布优先：刚刚创建的企划A排第一
                .body("data.list[0].projectId", equalTo("4001"))
                .body("data.list[1].projectId", equalTo("4002"));
    }

    @Test
    void shouldSortGroupsByMostPending() {
        seedPlazaFixture();
        // 企划B追加一个条目共2个待接取 → 缺员最多
        insertItemWithFlow(4004L, 4002L, "第03卷", 4004L);
        insertNode(4006L, 4004L, 4004L, 4002L, "翻译", 1, 1);
        insertNode(4007L, 4004L, 4004L, 4002L, "校对", 1, 2);
        insertInstance(4006L, 4002L, 4004L, 4006L, 1, null, 3);
        insertInstance(4007L, 4002L, 4004L, 4007L, 1, null, 3);
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("sortBy", "MOST")
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list[0].projectId", equalTo("4002"))
                .body("data.list[0].pendingCount", equalTo(3))
                .body("data.list[1].projectId", equalTo("4001"));
    }

    @Test
    void shouldOnlyExposeCurrentStageInstances() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token).get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                // 第04话阶段2（嵌字，实例4003）未解锁，不返回
                .body("data.list[1].tasks[0].instanceId", equalTo("4002"))
                .body("data.list[1].tasks.size()", equalTo(2));
    }

    @Test
    void shouldFilterPlazaByKeyword() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("keyword", "第05话")
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(1))
                .body("data.list[0].projectId", equalTo("4001"))
                .body("data.list[0].tasks.size()", equalTo(1))
                .body("data.list[0].tasks[0].instanceId", equalTo("4004"));
    }

    @Test
    void shouldFilterPlazaByProjectType() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("projectType", "小说")
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(1))
                .body("data.list[0].projectId", equalTo("4002"));
    }

    @Test
    void shouldPaginatePlazaGroups() {
        seedPlazaFixture();
        // 企划C（1天前创建）：等待排序位于企划B与企划A之间
        insertProjectType(4002L, 4000L, "美术");
        insertProject(4003L, 4000L, 4002L, "第三企划", 1);
        insertItemWithFlow(4005L, 4003L, "第01话", 4005L);
        insertNode(4008L, 4005L, 4005L, 4003L, "翻译", 1, 1);
        insertInstance(4008L, 4003L, 4005L, 4008L, 1, null, 1);
        String token = loginAs("plazauser", "123456");

        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 2)
                .get("/app/task/instance/plaza")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(3))
                .body("data.list.size()", equalTo(2))
                .body("data.list[0].projectId", equalTo("4002"))
                .body("data.list[1].projectId", equalTo("4003"));

        authRequest(token)
                .queryParam("pageNum", 2)
                .queryParam("pageSize", 2)
                .get("/app/task/instance/plaza")
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(3))
                .body("data.list.size()", equalTo(1))
                .body("data.list[0].projectId", equalTo("4001"));
    }

    @Test
    void shouldNotReturnOtherOrganizationTasks() {
        seedPlazaFixture();
        createOrganization(4001L, "其他组织");
        insertProjectType(4010L, 4001L, "漫画");
        insertProject(4010L, 4001L, 4010L, "他组织企划", 0);
        insertItemWithFlow(4010L, 4010L, "他组织条目", 4010L);
        insertNode(4010L, 4010L, 4010L, 4010L, "翻译", 1, 1);
        insertInstance(4010L, 4010L, 4010L, 4010L, 1, null, 0);
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token).get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(2));
    }

    @Test
    void shouldReturnEmptyPageWhenNoPendingTasks() {
        seedPlazaFixture();
        // 所有实例都已完成（阶段1双槽全部完成 → 条目无当前缺人阶段）
        jdbcTemplate.update("UPDATE `task_instance` SET status = 3, assignee_id = 4001 WHERE id IN (4002, 4003, 4004, 4005)");
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token).get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.total", equalTo(0))
                .body("data.list.size()", equalTo(0));
    }

    // ========== 失败路径 ==========

    @Test
    void shouldFailListPlazaWithoutAuth() {
        Response response = request().get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldFailListPlazaWithInvalidPageSize() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("pageSize", 101)
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailListPlazaWithUnknownProjectType() {
        seedPlazaFixture();
        String token = loginAs("plazauser", "123456");

        Response response = authRequest(token)
                .queryParam("projectType", "不存在")
                .get("/app/task/instance/plaza");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }
}

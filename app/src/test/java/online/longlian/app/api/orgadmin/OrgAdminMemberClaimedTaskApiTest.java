package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;

/**
 * 组织管理端组员「当前接取的任务」：成员列表列数量与任务列表弹层数据。
 */
public class OrgAdminMemberClaimedTaskApiTest extends BaseApiTest {

    /**
     * 接取任务按进行中/等待中拆分，并带上接取时间；停用企划上仍挂名的任务也返回
     */
    @Test
    void shouldListClaimedTasksByBucket() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "alan", "123456", "alan@example.com");
        jdbcTemplate.update("UPDATE `user` SET nickname = ? WHERE id = ?", "阿岚", 2L);
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        seedTasks();
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .get("/orgadmin/members/2/claimed-tasks")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.memberId", equalTo("2"))
                .body("data.userId", equalTo("2"))
                .body("data.totalCount", equalTo(3))
                .body("data.submittableCount", equalTo(2))
                .body("data.waitingCount", equalTo(1))
                .body("data.list", hasSize(3))
                .body("data.list[0].instanceId", equalTo("2"))
                .body("data.list[0].bucket", equalTo("WAITING"))
                .body("data.list[0].waitingStageName", equalTo("翻译"))
                .body("data.list[0].claimedAt", equalTo("2026-10-01 10:00:00"))
                .body("data.list[1].instanceId", equalTo("4"))
                .body("data.list[1].bucket", equalTo("SUBMITTABLE"))
                .body("data.list[1].waitingStageName", nullValue())
                .body("data.list[1].serialStep", equalTo(2))
                .body("data.list[2].instanceId", equalTo("5"))
                .body("data.list[2].projectName", equalTo("停用企划"))
                .body("data.list[2].bucket", equalTo("SUBMITTABLE"));
    }

    /**
     * 成员列表新增列：当前接取的任务数量
     */
    @Test
    void shouldReportClaimedTaskCountInMemberList() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "alan", "123456", "alan@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        seedTasks();
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/members")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.find { it.userId == '2' }.claimedTaskCount", equalTo(3))
                .body("data.list.find { it.userId == '1' }.claimedTaskCount", equalTo(0));
    }

    /**
     * 没有接取任务的成员返回空列表和 0 计数
     */
    @Test
    void shouldReturnEmptyWhenMemberHasNoClaimedTask() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "alan", "123456", "alan@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .get("/orgadmin/members/2/claimed-tasks")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.totalCount", equalTo(0))
                .body("data.submittableCount", equalTo(0))
                .body("data.waitingCount", equalTo(0))
                .body("data.list", hasSize(0));
    }

    /**
     * 不能查询其他组织的成员
     */
    @Test
    void shouldRejectMemberOfAnotherOrganization() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "另一组");
        createOrganizationMember(9L, 2L, 1L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .get("/orgadmin/members/9/claimed-tasks")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /**
     * 不存在的成员
     */
    @Test
    void shouldFailForMissingMember() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .get("/orgadmin/members/999/claimed-tasks")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 普通组员不能进入组织管理端接口
     */
    @Test
    void shouldRejectRegularMember() {
        createUserWithOrganization(1L, "regular", "123456", "regular@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("regular", "123456");

        authRequest(token)
                .get("/orgadmin/members/1/claimed-tasks")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    private void seedTasks() {
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                        + "VALUES (1, 1, 1, '夏日重现', '', '{}', 0, '', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, resource_status, creator_id) "
                        + "VALUES (2, 1, 1, '停用企划', '', '{}', 0, '', 1, 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                        + "VALUES (3, 2, 1, '别的组', '', '{}', 0, '', 1, 1)");
        jdbcTemplate.update("INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES (1, 1, '原子', '[]', 1, 1)");
        insertItem(1, 1, "第12话");
        insertItem(2, 1, "第13话");
        insertItem(3, 2, "停用里的项目");
        insertItem(5, 3, "别组项目");
        insertItem(6, 1, "已删除项目");
        jdbcTemplate.update("UPDATE item SET deleted_at = NOW() WHERE id = 6");
        insertNode(1, 1, 1, 1, "翻译", 1, 1);
        insertNode(2, 1, 1, 1, "嵌字", 2, 1);
        insertNode(3, 2, 2, 1, "校对", 1, 1);
        insertNode(4, 2, 2, 1, "校对", 2, 1);
        insertNode(5, 3, 3, 2, "图源", 1, 1);
        insertNode(7, 5, 5, 3, "翻译", 1, 1);
        insertNode(8, 6, 6, 1, "嵌字", 1, 1);
        insertInstance(1, 1, 1, 1, 1, null, 1, null);
        insertInstance(2, 1, 1, 2, 1, 2L, 2, "2026-10-01 10:00:00");
        insertInstance(3, 1, 2, 3, 2, 2L, 3, "2026-10-01 09:00:00");
        insertInstance(4, 1, 2, 4, 2, 2L, 2, "2026-10-02 10:00:00");
        insertInstance(5, 2, 3, 5, 3, 2L, 2, "2026-10-03 10:00:00");
        insertInstance(7, 3, 5, 7, 5, 2L, 2, "2026-10-01 08:00:00");
        insertInstance(8, 1, 6, 8, 6, 2L, 2, "2026-10-01 07:00:00");
    }

    private void insertItem(long id, long projectId, String title) {
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (?, ?, ?, 0, 1)",
                id, projectId, title);
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (?, ?, ?, 1, '任务流')",
                id, id, projectId);
    }

    private void insertNode(long id, long flowId, long itemId, long projectId, String name, int sort, int parallelSort) {
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) "
                        + "VALUES (?, ?, ?, ?, 1, ?, '[]', ?, ?)",
                id, flowId, itemId, projectId, name, sort, parallelSort);
    }

    private void insertInstance(long id, long projectId, long itemId, long nodeId, long flowId,
                                Long assigneeId, int status, String claimedAt) {
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status, claimed_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                id, projectId, itemId, nodeId, flowId, assigneeId, status, claimedAt);
    }
}

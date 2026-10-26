package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ItemDeleteApiTest extends BaseApiTest {

    /**
     * 企划负责人可直接删除没有已提交任务的项目，并级联软删任务流、节点、实例，递减模板引用、废弃任务附件。
     * 已打回或已重置的历史提交不阻止删除，且提交记录本身保留。
     */
    @Test
    void shouldDeleteItemWithoutSubmittedTaskAndCascade() {
        String token = creator();
        insertProject(1);
        insertItem(1L, "可删除项目", 1L);
        insertItem(2L, "保留项目", 1L);
        insertFlow(1L, 1L);
        insertNode(1L, 1L);
        insertInstance(1L, 1L, 2);
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, status) "
                        + "VALUES (1, 1, 1, 1, 1, 1, 1, 2), (2, 1, 1, 1, 1, 1, 2, 3)");
        jdbcTemplate.update(
                "INSERT INTO resource (id, org_id, storage_type, storage_key, file_name, file_ext, file_size, biz_type, biz_id, process_status, creator_id) "
                        + "VALUES (1, 1, 1, 'task_submit/1.png', '成果.png', 'png', 1, 'task_submit', 1, ?, 1)",
                FileProcessStatus.Activated.getCode());
        jdbcTemplate.update("UPDATE task_template SET ref_count = 2 WHERE id = 1");
        authRequest(token)
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("删除成功"));

        assertDeleted("item", 1L);
        assertDeleted("item_task_flow", 1L);
        assertDeleted("item_task_node", 1L);
        assertDeleted("task_instance", 1L);
        assertThat(deletedAt("item", 2L)).isNull();
        assertThat(activeCount("project", 1L)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT ref_count FROM task_template WHERE id = 1", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = 1", Integer.class))
                .isEqualTo(FileProcessStatus.Deprecated.getCode());
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM task_submission WHERE deleted_at IS NULL", Integer.class))
                .isEqualTo(2);
        authRequest(token)
                .get("/app/projects/1/items")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].title", equalTo("保留项目"));
    }

    /**
     * 仍有已提交任务时拒绝直接删除，并提示改走删除申请。
     */
    @Test
    void shouldRejectDeleteItemWhenSubmittedTaskExists() {
        String token = creator();
        insertProject(1);
        insertItem(1L, "有提交项目", 1L);
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, status) "
                        + "VALUES (1, 1, 1, 1, 1, 1, 1, 1)");

        authRequest(token)
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", equalTo("参数错误,项目存在已提交任务，请提交删除申请"));

        assertThat(deletedAt("item", 1L)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT ref_count FROM task_template WHERE id = 1", Integer.class)).isEqualTo(1);
    }

    /**
     * 非企划负责人不能删除项目。
     */
    @Test
    void shouldRejectDeleteItemByNonCreator() {
        creator();
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 2L);
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        insertProject(1);
        insertItem(1L, "别人的项目", 1L);

        authRequest(loginAs("member", "123456"))
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()))
                .body("msg", equalTo("无权操作"));

        assertThat(deletedAt("item", 1L)).isNull();
    }

    /**
     * 未登录不能删除项目。
     */
    @Test
    void shouldFailDeleteItemWithoutAuth() {
        request()
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 项目不存在、不属于当前企划，或企划已禁用时，删除失败且不改数据。
     */
    @Test
    void shouldFailDeleteMissingOrInvisibleItem() {
        String token = creator();
        insertProject(0);
        insertItem(1L, "禁用企划项目", 1L);

        authRequest(token)
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(deletedAt("item", 1L)).isNull();

        jdbcTemplate.update("UPDATE project SET resource_status = 1 WHERE id = 1");
        authRequest(token)
                .delete("/app/projects/1/items/99")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));

        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, status, resource_status, creator_id) VALUES (2, 1, 1, '另一企划', 1, 1, 1)");
        authRequest(token)
                .delete("/app/projects/2/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(deletedAt("item", 1L)).isNull();
    }

    private String creator() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        return loginAs("orgadmin", "123456");
    }

    private void insertProject(int resourceStatus) {
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, status, resource_status, creator_id) VALUES (1, 1, 1, '测试企划', 1, ?, 1)",
                resourceStatus);
        jdbcTemplate.update(
                "INSERT INTO task_template (id, org_id, name, status, scope, ref_count, creator_id) VALUES (1, 1, '模板', 1, 2, 1, 1)");
    }

    private void insertItem(Long itemId, String title, Long templateId) {
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (?, 1, ?, ?, 1)",
                itemId, title, templateId);
    }

    private void insertFlow(Long flowId, Long itemId) {
        jdbcTemplate.update(
                "INSERT INTO item_task_flow (id, item_id, project_id, task_template_id, name) VALUES (?, ?, 1, 1, '任务流')",
                flowId, itemId);
    }

    private void insertNode(Long nodeId, Long itemId) {
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, sort, parallel_sort) "
                        + "VALUES (?, ?, ?, 1, 0, '节点', 1, 1)",
                nodeId, itemId, itemId);
    }

    private void insertInstance(Long instanceId, Long itemId, int status) {
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, assignee_id, status) "
                        + "VALUES (?, 1, ?, ?, ?, 1, ?)",
                instanceId, itemId, itemId, itemId, status);
    }

    private Integer activeCount(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + table + "` WHERE id = ? AND deleted_at IS NULL", Integer.class, id);
    }

    private LocalDateTime deletedAt(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT deleted_at FROM `" + table + "` WHERE id = ?", LocalDateTime.class, id);
    }

    private void assertDeleted(String table, Long id) {
        assertThat(deletedAt(table, id)).isNotNull();
        assertThat(activeCount(table, id)).isZero();
    }
}

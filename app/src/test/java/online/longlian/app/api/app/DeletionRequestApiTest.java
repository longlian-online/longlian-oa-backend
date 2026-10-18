package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class DeletionRequestApiTest extends BaseApiTest {

    @Test
    void shouldApproveProjectDeletionRequestAndSoftDelete() {
        String token = creator();
        insertProject("有项目企划");
        insertItem(1L);

        authRequest(token)
                .body(Map.of("reason", "不再继续"))
                .post("/app/projects/1/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("已提交删除申请"));

        String requestId = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/orgadmin/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1))
                .body("data.list[0].targetType", equalTo("PROJECT"))
                .body("data.list[0].title", equalTo("有项目企划"))
                .body("data.list[0].reason", equalTo("不再继续"))
                .extract()
                .path("data.list[0].id");

        authRequest(token)
                .body(Map.of("status", "APPROVED"))
                .patch("/orgadmin/deletion-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("审核完成"));

        assertThat(activeCount("project", 1L)).isZero();
        assertThat(activeCount("item", 1L)).isZero();
    }

    @Test
    void shouldRejectProjectDeletionRequestWithoutDeleting() {
        String token = creator();
        insertProject("保留企划");
        insertItem(1L);
        authRequest(token).body(Map.of("reason", "误操作")).post("/app/projects/1/deletion-requests");
        Long requestId = jdbcTemplate.queryForObject("SELECT id FROM deletion_request", Long.class);

        authRequest(token)
                .body(Map.of("status", "REJECTED"))
                .patch("/orgadmin/deletion-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(activeCount("project", 1L)).isEqualTo(1);
        assertThat(activeCount("item", 1L)).isEqualTo(1);
    }

    @Test
    void shouldRejectDuplicatePendingDeletionRequest() {
        String token = creator();
        insertProject("重复申请");
        insertItem(1L);
        authRequest(token).body(Map.of("reason", "第一次")).post("/app/projects/1/deletion-requests")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).body(Map.of("reason", "第二次")).post("/app/projects/1/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,已提交删除申请，请等待审核"));
    }

    @Test
    void shouldApproveItemDeletionRequestWhenSubmissionExists() {
        String token = creator();
        insertProject("有提交的企划");
        insertItem(1L);
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, status) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, 1L, 1L, 1L, 1, 1);

        authRequest(token)
                .delete("/app/projects/1/items/1")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", equalTo("参数错误,项目存在已提交任务，请提交删除申请"));

        authRequest(token)
                .body(Map.of("reason", "提交作废"))
                .post("/app/projects/1/items/1/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/orgadmin/deletion-requests")
                .then()
                .body("data.list[0].targetType", equalTo("ITEM"));

        Long requestId = jdbcTemplate.queryForObject(
                "SELECT id FROM deletion_request WHERE target_type = 2", Long.class);
        authRequest(token)
                .body(Map.of("status", "APPROVED"))
                .patch("/orgadmin/deletion-requests/" + requestId)
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(activeCount("item", 1L)).isZero();
        assertThat(activeCount("project", 1L)).isEqualTo(1);
    }

    @Test
    void shouldRejectDeletionRequestFromNonCreator() {
        creator();
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 2L);
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        insertProject("别人的企划");
        insertItem(1L);

        authRequest(loginAs("member", "123456"))
                .body(Map.of("reason", "无权"))
                .post("/app/projects/1/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM deletion_request", Integer.class)).isZero();
    }

    private String creator() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        return loginAs("orgadmin", "123456");
    }

    private void insertProject(String title) {
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "漫画", 1, 1L);
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, status, resource_status, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, title, 1, 1, 1L);
    }

    private void insertItem(Long itemId) {
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (?, ?, ?, ?, ?)",
                itemId, 1L, "第一章", 1L, 1L);
    }

    private Integer activeCount(String table, Long id) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `" + table + "` WHERE id = ? AND deleted_at IS NULL", Integer.class, id);
    }
}

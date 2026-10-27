package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class AdminReviewNotificationApiTest extends BaseApiTest {

    @Test
    void shouldNotifyEnabledAdministratorsWhenJoining() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        addMember(2L, "admin", "admin@example.com", "管理员", "ORG_ADMIN", 1);
        addMember(3L, "member", "member@example.com", "组员", "ORG_USER", 1);
        addMember(4L, "paused", "paused@example.com", "停用", "ORG_ADMIN", 0);
        createUserWithOrganization(5L, "joiner", "123456", "joiner@example.com", 5L, 2L, "ORG_USER");
        jdbcTemplate.update("UPDATE `user` SET nickname = ? WHERE id = ?", "新同学", 5L);
        useRequestOrg(1L);

        String inviteCode = authRequest(loginAs("owner", "123456"), 1L)
                .body(inviteExpiryBody())
                .post("/orgadmin/members/invite-codes/join-org")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .extract()
                .path("data.inviteCode");
        userRequest(loginAs("joiner", "123456"))
                .body(Map.of("inviteCode", inviteCode))
                .post("/app/user/organizations/join-by-invite")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(recipients(4)).containsExactly(1L, 2L);
        assertThat(outboxCount()).isEqualTo(2);
        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .statusCode(200)
                .body("data.list[0].kind", equalTo("JOIN_APPLICATION"))
                .body("data.list[0].source.applicantName", equalTo("新同学"))
                .body("data.list[0].source.projectId", nullValue());
    }

    @Test
    void shouldNotifyEnabledAdministratorsWhenDeletionIsRequested() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        addMember(2L, "admin", "admin@example.com", "管理员", "ORG_ADMIN", 1);
        addMember(3L, "member", "member@example.com", "组员", "ORG_USER", 1);
        addMember(4L, "paused", "paused@example.com", "停用", "ORG_ADMIN", 0);
        useRequestOrg(1L);
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "漫画", 1, 2L);
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, status, resource_status, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                1L, 1L, 1L, "有项目企划", 1, 1, 2L);
        jdbcTemplate.update(
                "INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (?, ?, ?, ?, ?)",
                1L, 1L, "第一章", 1L, 2L);

        authRequest(loginAs("admin", "123456"), 1L)
                .body(Map.of("reason", "不再继续"))
                .post("/app/projects/1/deletion-requests")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(recipients(5)).containsExactly(1L, 2L);
        assertThat(outboxCount()).isEqualTo(2);
        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .statusCode(200)
                .body("data.list[0].kind", equalTo("DELETION_REQUEST"))
                .body("data.list[0].source.requesterName", equalTo("管理员"))
                .body("data.list[0].source.targetType", equalTo("PROJECT"))
                .body("data.list[0].source.targetTitle", equalTo("有项目企划"));
    }

    private void addMember(long userId, String username, String email, String nickname, String role, int status) {
        createTestUser(userId, username, "123456", email);
        jdbcTemplate.update("UPDATE `user` SET nickname = ?, default_org_id = ? WHERE id = ?", nickname, 1L, userId);
        createOrganizationMember(userId, 1L, userId, role);
        jdbcTemplate.update("UPDATE organization_member SET status = ? WHERE id = ?", status, userId);
    }

    private List<Long> recipients(int kind) {
        return jdbcTemplate.queryForList(
                "SELECT recipient_user_id FROM user_notification WHERE kind = ? ORDER BY recipient_user_id",
                Long.class, kind);
    }

    private Integer outboxCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM outbox_message", Integer.class);
    }
}

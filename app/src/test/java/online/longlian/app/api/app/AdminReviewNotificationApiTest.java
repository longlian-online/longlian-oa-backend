package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
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
        createResource(700L, 2L, 5L);
        jdbcTemplate.update("UPDATE `user` SET nickname = ?, avatar_file_id = ? WHERE id = ?", "新同学", 700L, 5L);
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
                .body("data.list[0].source.projectId", nullValue())
                .body("data.list[0].thumbnailUrl", containsString("700.png"))
                .body("data.list[0].thumbnailUrl", containsString("eo-img.resize=w/144/h/144"));

        jdbcTemplate.update("UPDATE resource SET process_status = 0 WHERE id = 700");
        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].source.applicantName", equalTo("新同学"))
                .body("data.list[0].thumbnailUrl", nullValue());
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

        createResource(700L, 1L, 2L);
        createResource(701L, 1L, 2L);
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id = 700, cover_file_id = 701 WHERE id = 1");

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
                .body("data.list[0].source.targetTitle", equalTo("有项目企划"))
                .body("data.list[0].thumbnailUrl", containsString("700.png"))
                .body("data.list[0].thumbnailUrl", containsString("eo-img.resize=w/144/h/144"));

        jdbcTemplate.update("UPDATE resource SET process_status = 0 WHERE id = 700");
        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].thumbnailUrl", containsString("701.png"));
    }

    // 已审核项目删除通知不依赖仍可读取项目，图片取自尚存的所属企划。
    @Test
    void shouldKeepProjectThumbnailAfterDeletionOfRequestedItem() {
        createDeletionNotification();
        createResource(700L, 1L, 1L);
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id = 700 WHERE id = 1");
        jdbcTemplate.update("UPDATE deletion_request SET status = 1, reviewed_at = NOW() WHERE id = 10");
        jdbcTemplate.update("UPDATE item SET deleted_at = NOW() WHERE id = 1");

        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].source.targetType", equalTo("ITEM"))
                .body("data.list[0].source.targetTitle", equalTo("第一章"))
                .body("data.list[0].thumbnailUrl", containsString("700.png"));
    }

    // 企划或资源不属于当前组织、不可用时，保留删除申请快照但不暴露图片。
    @ParameterizedTest
    @ValueSource(strings = {
            "UPDATE project SET org_id = 2 WHERE id = 1",
            "UPDATE project SET resource_status = 0 WHERE id = 1",
            "UPDATE project SET deleted_at = NOW() WHERE id = 1",
            "UPDATE resource SET org_id = 2 WHERE id = 700"
    })
    void shouldKeepDeletionTextWithoutInaccessibleProjectImages(String change) {
        createDeletionNotification();
        createResource(700L, 1L, 1L);
        jdbcTemplate.update("UPDATE project SET thumbnail_file_id = 700 WHERE id = 1");
        jdbcTemplate.update(change);

        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].source.targetTitle", equalTo("第一章"))
                .body("data.list[0].thumbnailUrl", nullValue());
    }

    // 注册待审没有关联用户，不能按用户名匹配已有账号来获取头像。
    @Test
    void shouldKeepRegistrationApplicationWithoutBorrowingExistingUserAvatar() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        createResource(700L, 1L, 1L);
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 700 WHERE id = 1");
        jdbcTemplate.update("INSERT INTO group_application (id, org_id, otp_id, username, nickname) VALUES (10, 1, 1, 'owner', '注册待审')");
        jdbcTemplate.update("INSERT INTO user_notification (id, org_id, recipient_user_id, kind, source_id, trigger_id) VALUES (20, 1, 1, 4, 10, 10)");

        authRequest(loginAs("owner", "123456"), 1L)
                .get("/app/notifications")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].source.applicantName", equalTo("注册待审"))
                .body("data.list[0].thumbnailUrl", nullValue());
    }

    private void createDeletionNotification() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, creator_id) VALUES (1, 1, '漫画', 1)");
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, creator_id) VALUES (1, 1, 1, '企划', 1)");
        jdbcTemplate.update("INSERT INTO item (id, project_id, title, task_template_id, creator_id) VALUES (1, 1, '第一章', 1, 1)");
        jdbcTemplate.update("INSERT INTO deletion_request (id, org_id, target_type, target_id, project_id, target_title, reason, requester_id) VALUES (10, 1, 2, 1, 1, '第一章', '不再继续', 1)");
        jdbcTemplate.update("INSERT INTO user_notification (id, org_id, recipient_user_id, kind, source_id, trigger_id) VALUES (20, 1, 1, 5, 10, 10)");
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

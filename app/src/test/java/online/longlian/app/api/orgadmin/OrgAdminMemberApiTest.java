package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;

public class OrgAdminMemberApiTest extends BaseApiTest {

    // ========== 入组申请 ==========

    /**
     * 分页查询待审核入组申请列表成功
     */
    @Test
    void shouldListApplications() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createTestUser(2L, "applyuser", "123456", "apply@example.com");
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = ?", 2L);

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/members/applications");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 审核入组申请成功
     */
    @Test
    void shouldReviewApplication() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");



        // 插入待审核的入组申请
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, password_hash, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, null, 0, 0, "applyuser", "申请人", "apply@example.com", passwordEncoder.encode("123456")
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "applicationStatus", "APPROVED",
                        "reviewRemark", "审核通过"
                ))
                .put("/orgadmin/members/applications/1/review");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        Long userId = jdbcTemplate.queryForObject(
                "SELECT user_id FROM group_application WHERE id = ? AND status = 1 AND password_hash IS NULL",
                Long.class, 1L);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT username FROM `user` WHERE id = ?", String.class, userId)).isEqualTo("applyuser");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE org_id = ? AND user_id = ? AND status = 1",
                Integer.class, 1L, userId)).isEqualTo(1);
    }

    /**
     * 审核入组申请成功（REJECTED 路径，覆盖 rejectApplication 分支）
     */
    @Test
    void shouldRejectApplicationSuccessfully() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createTestUser(2L, "applyuser", "123456", "apply@example.com");
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = ?", 2L);

        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, 0, 0, "applyuser", "申请人", "apply@example.com"
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "applicationStatus", "REJECTED",
                        "reviewRemark", "暂不通过"
                ))
                .put("/orgadmin/members/applications/1/review");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 审核已处理申请应失败（覆盖 validatePendingApplication status!=PENDING 分支）
     */
    @Test
    void shouldFailReviewAlreadyReviewedApplication() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        createTestUser(2L, "applyuser", "123456", "apply@example.com");
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = ?", 2L);

        // status=1 → APPROVED，非 PENDING
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, 1, 0, "applyuser", "申请人", "apply@example.com"
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "applicationStatus", "APPROVED",
                        "reviewRemark", "审核通过"
                ))
                .put("/orgadmin/members/applications/1/review");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 待审核状态不是审核结果，必须在写入前拒绝且保留注册密码快照。 */
    @Test
    void shouldRejectPendingReviewResultWithoutChangingApplication() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        String hash = passwordEncoder.encode("123456");
        jdbcTemplate.update(
                "INSERT INTO group_application (id, org_id, application_type, status, username, email, password_hash) " +
                        "VALUES (?, ?, 0, 0, ?, ?, ?)", 1L, 1L, "candidate", "candidate@example.com", hash);

        authRequest(token).body(Map.of("applicationStatus", "PENDING"))
                .put("/orgadmin/members/applications/1/review")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id = ?", Integer.class, 1L)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM group_application WHERE id = ?", String.class, 1L)).isEqualTo(hash);
        assertThat(jdbcTemplate.queryForObject("SELECT reviewer_id FROM group_application WHERE id = ?", Long.class, 1L)).isNull();
    }

    /**
     * 审核通过 EXISTING_USER 类型申请时申请人已是组织成员应失败
     */
    @Test
    void shouldFailApproveApplicationWhenUserAlreadyMember() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        // user2 已是 org1 的成员
        createTestUser(2L, "user2", "123456", "user2@example.com");
        jdbcTemplate.update(
                "INSERT INTO `organization_member` (id, org_id, user_id, org_role, status) VALUES (?, ?, ?, ?, 1)",
                2L, 1L, 2L, "ORG_USER"
        );

        // PENDING 申请，application_type=1 EXISTING_USER，user_id=2（已是成员）
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, 0, 1, "user2", "用户2", "user2@example.com"
        );

        Response response = authRequest(token)
                .body(Map.of(
                        "applicationStatus", "APPROVED",
                        "reviewRemark", "审核通过"
                ))
                .put("/orgadmin/members/applications/1/review");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM group_application WHERE id = ?", Integer.class, 1L)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewer_id FROM group_application WHERE id = ?", Long.class, 1L)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE org_id = ? AND user_id = ?",
                Integer.class, 1L, 2L)).isEqualTo(1);
    }

    // ========== 组员列表 ==========

    /**
     * 分页查询组员列表成功
     */
    @Test
    void shouldListMembers() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("pageNum", 1, "pageSize", 10))
                .post("/orgadmin/members");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"ASC", "DESC"})
    void shouldPageMembersWithoutDuplicatesWhenJoinedTimesTie(String orderDir) {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        for (long id = 2; id <= 43; id++) {
            createTestUser(id, "member" + id, "123456", "member" + id + "@example.com");
            createOrganizationMember(id, 1L, id, "ORG_USER");
        }
        createOrganization(2L, "其他组织");
        createOrganizationMember(44L, 2L, 1L, "ORG_USER");
        jdbcTemplate.update("UPDATE organization_member SET joined_at = '2026-01-01 12:00:00'");
        jdbcTemplate.update("UPDATE organization_member SET joined_at = '2026-01-02 12:00:00' WHERE id = 1");
        String token = loginAs("orgadmin", "123456");
        boolean ascending = "ASC".equals(orderDir);
        List<Long> expected = new ArrayList<>(LongStream.rangeClosed(2, 43)
                .map(id -> ascending ? id : 45 - id).boxed().toList());
        expected.add(ascending ? expected.size() : 0, 1L);
        List<Long> actual = new ArrayList<>();

        for (int pageNum = 1; pageNum <= 6; pageNum++) {
            Map<String, Object> request = new HashMap<>(Map.of("pageNum", pageNum, "pageSize", 10));
            if (orderDir != null) {
                request.put("orderDir", orderDir);
            }
            Response response = authRequest(token).body(request).post("/orgadmin/members");
            response.then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.total", equalTo(43));
            List<Long> ids = response.jsonPath().getList("data.list.id", Long.class);
            List<Long> userIds = response.jsonPath().getList("data.list.userId", Long.class);
            int start = Math.min((pageNum - 1) * 10, 43);
            List<Long> expectedPage = expected.subList(start, Math.min(start + 10, 43));
            assertThat(ids).containsExactlyElementsOf(expectedPage);
            assertThat(userIds).containsExactlyElementsOf(expectedPage);
            actual.addAll(ids);
        }
        assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
    }

    /**
     * 查询组员各原子任务提交数成功
     */
    @Test
    void shouldGetMemberBaseTaskSubmitCounts() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .get("/orgadmin/members/1/base-tasks/submit-counts");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 重置和打回的提交已从总数回退，原子任务明细不得再计入。
     */
    @Test
    void shouldExcludeResetAndRejectedSubmissionsFromBaseTaskCounts() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE organization_member SET submit_count = 1 WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, meta_schema, status, creator_id) VALUES "
                        + "(1, 1, '校对', '[]', 1, 1), (2, 1, '翻译', '[]', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO item_task_node (id, item_task_flow_id, item_id, project_id, base_task_id, name, meta_schema, sort, parallel_sort) VALUES "
                        + "(1, 1, 1, 1, 1, '校对节点', '[]', 1, 1), (2, 1, 1, 1, 2, '翻译节点', '[]', 2, 1)");
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, submitter_id, version_no, metadata, status) VALUES "
                        + "(1, 1, 1, 1, 1, 1, 1, '{}', 3), "
                        + "(2, 1, 1, 2, 1, 1, 1, '{}', 2), "
                        + "(3, 1, 1, 3, 2, 1, 1, '{}', 1)");
        String token = loginAs("orgadmin", "123456");

        authRequest(token)
                .get("/orgadmin/members/1/base-tasks/submit-counts")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.totalSubmitCount", equalTo(1))
                .body("data.list", hasSize(2))
                .body("data.list[0].baseTaskName", equalTo("校对"))
                .body("data.list[0].submitCount", equalTo(0))
                .body("data.list[1].baseTaskName", equalTo("翻译"))
                .body("data.list[1].submitCount", equalTo(1));
    }


    /**
     * 启用/禁用组员成功
     */
    @Test
    void shouldChangeMemberStatus() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        // 创建第二个 MEMBER 角色成员用于测试禁用
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/members/2/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("状态修改成功"));
    }

    // ========== 邀请码 ==========

    /**
     * 生成加入组织邀请码成功
     */
    @Test
    void shouldGenerateJoinOrgInviteCode() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .post("/orgadmin/members/invite-codes/join-org");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.inviteCode", notNullValue())
                .body("data.expireAt", notNullValue());
    }

    // ========== 认证失败 ==========

    /**
     * 未认证访问成员管理接口失败
     */
    @Test
    void shouldFailWithoutAuth() {
        Response response = request()
                .post("/orgadmin/members/applications");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
    @Test
    void shouldPromoteAndDemoteMemberWithinOrganization() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        createTestUser(3L, "secondadmin", "123456", "secondadmin@example.com");
        createOrganization(3L, "其他组织");
        createOrganizationMember(3L, 3L, 3L, "ORG_ADMIN");
        createTestUser(4L, "orgadmin", "123456", "orgadmin@example.com");
        createOrganizationMember(4L, 1L, 4L, "ORG_ADMIN");
        String token = loginAs("owner", "123456");

        authRequest(token).body(Map.of("orgRole", "ORG_ADMIN")).patch("/orgadmin/members/2/role")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String promotedToken = loginAs("member", "123456");
        authRequest(promotedToken).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/2/role")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/3/role")
                .then().statusCode(200).body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
        authRequest(token).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/1/role")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,不能修改组织所有者的角色"));
        authRequest(token).body(Map.of("orgRole", "ORG_OWNER")).patch("/orgadmin/members/2/role")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,不能分配组织所有者"));
        authRequest(loginAs("orgadmin", "123456")).body(Map.of("orgRole", "ORG_USER"))
                .patch("/orgadmin/members/2/role")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/4/role")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 1", String.class)).isEqualTo("ORG_OWNER");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 2", String.class)).isEqualTo("ORG_USER");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 3", String.class)).isEqualTo("ORG_ADMIN");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 4", String.class)).isEqualTo("ORG_USER");
    }

    @Test
    void shouldRejectInvalidRoleRequest() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");

        authRequest(token).body(Map.of("orgRole", "SUPER_ADMIN"))
                .patch("/orgadmin/members/2/role")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 2", String.class)).isEqualTo("ORG_USER");
    }

    @Test
    void shouldRejectOrdinaryMemberChangingRole() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("member", "123456");

        authRequest(token).body(Map.of("orgRole", "ORG_ADMIN"))
                .patch("/orgadmin/members/2/role")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT org_role FROM organization_member WHERE id = 2", String.class)).isEqualTo("ORG_USER");
    }

}

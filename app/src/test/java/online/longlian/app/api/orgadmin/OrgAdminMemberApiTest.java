package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
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

        createTestUser(2L, "applyuser", "123456", "apply@example.com");
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = 2");

        // 插入待审核的入组申请
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, 0, 0, "applyuser", "申请人", "apply@example.com"
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
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }

    /**
     * 审核通过 EXISTING_USER 类型申请时申请人已是组织成员应失败
     * （覆盖 getExistingApplicationUser existedMember!=null 分支）
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
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
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
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 2L);
        createTestUser(3L, "secondadmin", "123456", "secondadmin@example.com");
        createOrganization(3L, "其他组织");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 3L, 3L);
        createOrganizationMember(3L, 3L, 3L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

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
                .body("msg", equalTo("组织至少保留一名管理员"));

        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id = 2", String.class), equalTo("ORG_USER"));
        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id = 3", String.class), equalTo("ORG_ADMIN"));
    }

    @Test
    void shouldResetMemberPasswordOnceWithoutPersistingPlaintext() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token).post("/orgadmin/members/2/password/reset");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.password", matchesPattern("[A-Za-z0-9]{12}"));
        String password = response.jsonPath().getString("data.password");

        request().body(Map.of("username", "member", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200).body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
        request().body(Map.of("username", "member", "password", password)).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE id = 2", String.class), not(containsString(password)));
    }

}

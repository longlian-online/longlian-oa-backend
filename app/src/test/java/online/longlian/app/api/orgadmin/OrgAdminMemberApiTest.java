package online.longlian.app.api.orgadmin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

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
                "INSERT INTO group_application (id, org_id, user_id, status, application_type, username, password, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 0L, 0, 0, "applyuser", passwordEncoder.encode("123456"), "申请人", "apply@example.com"
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

        jdbcTemplate.update(
                "INSERT INTO group_application (id, org_id, user_id, status, application_type, username, password, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 0L, 0, 0, "applyuser", passwordEncoder.encode("123456"), "申请人", "apply@example.com"
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

        // status=1 → APPROVED，非 PENDING
        jdbcTemplate.update(
                "INSERT INTO group_application (id, org_id, user_id, status, application_type, username, password, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 0L, 1, 0, "applyuser", passwordEncoder.encode("123456"), "申请人", "apply@example.com"
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
                "INSERT INTO organization_member (id, org_id, user_id, org_role, status) VALUES (?, ?, ?, ?, 1)",
                2L, 1L, 2L, "MEMBER"
        );

        // PENDING 申请，application_type=1 EXISTING_USER，user_id=2（已是成员）
        jdbcTemplate.update(
                "INSERT INTO group_application (id, org_id, user_id, status, application_type, username, password, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, 0, 1, "user2", passwordEncoder.encode("123456"), "用户2", "user2@example.com"
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
        createOrganizationMember(2L, 1L, 2L, "MEMBER");
        String token = loginAs("orgadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/members/2/status");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
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
}
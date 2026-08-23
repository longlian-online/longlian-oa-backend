package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.*;

public class UserApiTest extends BaseApiTest {

    // ========== 注册与创建组织 ==========

    /**
     * 通过邀请码注册并创建组织成功
     */
    @Test
    void shouldRegisterAndCreateOrganization() {
        // 创建超管并生成邀请码
        String adminToken = createRootAdmin();
        Response inviteResponse = authRequest(adminToken)
                .post("/admin/organizations/invite-codes/create-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        long userId = uniqueId();
        createEmailVerifyOTP("123456", userId, "newuser_" + userId + "@example.com");
        createOrganizationCreateInviteOTP(inviteCode);

        Response response = request()
                .body(Map.of(
                        "email", "newuser_" + userId + "@example.com",
                        "password", "123456",
                        "username", "newuser_" + userId,
                        "nickname", "新用户",
                        "inviteCode", inviteCode,
                        "code", "123456",
                        "orgName", "测试组织"
                ))
                .post("/app/user/register/create-organization");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @Test
    void shouldRejectLoginCodeWhenRegistering() {
        String adminToken = createRootAdmin();
        String inviteCode = authRequest(adminToken)
                .post("/admin/organizations/invite-codes/create-org")
                .then()
                .statusCode(200)
                .extract()
                .path("data.inviteCode");
        createOrganizationCreateInviteOTP(inviteCode);

        long otpId = 9001L;
        String email = "register-with-login-code@example.com";
        jdbcTemplate.update(
                "INSERT INTO one_time_password (id, code, expired_at, biz_type, status, creator_id) VALUES (?, ?, ?, ?, ?, ?)",
                otpId, "654321", LocalDateTime.now().plusMinutes(30), 3, 0, 0L
        );
        jdbcTemplate.update(
                "INSERT INTO email_verify_otp (id, otp_id, receiver, business_type, send_status) VALUES (?, ?, ?, ?, ?)",
                otpId, otpId, email, 0, 1
        );

        Response response = request()
                .body(Map.of(
                        "email", email,
                        "password", "123456",
                        "username", "wrongcodeuser",
                        "nickname", "错误验证码用户",
                        "inviteCode", inviteCode,
                        "code", "654321",
                        "orgName", "测试组织"
                ))
                .post("/app/user/register/create-organization");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }

    /**
     * 通过邀请码注册并加入组织成功
     */
    @Test
    void shouldRegisterAndJoinOrganization() {
        // 创建组织和管理员，由管理员生成加入组织邀请码
        long orgId = uniqueId();
        long adminUserId = System.currentTimeMillis();
        createUserWithOrganization(adminUserId, "orgadmin_" + adminUserId, "123456",
                "orgadmin_" + adminUserId + "@example.com",
                orgId, orgId, "ORG_ADMIN");
        String orgAdminToken = loginAs("orgadmin_" + adminUserId, "123456");

        Response inviteResponse = authRequest(orgAdminToken)
                .post("/orgadmin/members/invite-codes/join-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        // 新用户通过邀请码注册并加入组织
        long userId = uniqueId();
        createEmailVerifyOTP("123456", userId, "newuser_" + userId + "@example.com");

        Response createResponse = request()
                .body(Map.of(
                        "email", "newuser_" + userId + "@example.com",
                        "password", "123456",
                        "username", "newuser_" + userId,
                        "nickname", "新用户",
                        "inviteCode", inviteCode,
                        "code", "123456"
                ))
                .post("/app/user/register/join-organization");

        createResponse.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // ========== 邀请码查询 ==========

    /**
     * 获取邀请码对应的组织信息成功
     */
    @Test
    void shouldGetInviteInfo() {
        // 创建用户和组织，orgadmin 生成加入组织的邀请码（OrganizationUserInvite 类型）
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String adminToken = loginAs("orgadmin", "123456");

        Response inviteResponse = authRequest(adminToken)
                .post("/orgadmin/members/invite-codes/join-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        // 获取邀请码对应的组织信息
        Response response = request()
                .get("/app/user/register/join-organization/invite-info?inviteCode=" + inviteCode);

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.orgName", notNullValue());
    }

    /**
     * 使用无效邀请码获取组织信息失败
     */
    @Test
    void shouldFailWithInvalidInviteCode() {
        Response response = request()
                .get("/app/user/register/join-organization/invite-info?inviteCode=INVALID");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    // ========== 用户信息 ==========

    /**
     * 获取当前登录用户信息成功
     */
    @Test
    void shouldGetMyInfo() {
        // 创建用户和组织
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");

        // 登录获取token
        String token = loginAs("testuser", "123456");

        // 获取当前用户信息
        Response response = authRequest(token)
                .get("/app/user/");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 获取用户加入的组织列表成功
     */
    @Test
    void shouldGetOrganizations() {
        // 创建用户和组织
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");

        // 登录获取token
        String token = loginAs("testuser", "123456");

        // 获取用户加入的组织列表
        Response response = authRequest(token)
                .get("/app/user/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 组织操作 ==========

    /**
     * 已登录用户通过邀请码加入组织成功
     */
    @Test
    void shouldJoinOrganizationByInvite() {
        // 创建两个用户和对应的组织
        createUserWithOrganization(1L, "user1", "123456", "user1@example.com", 1L, 1L, "ORG_ADMIN");
        createUserWithOrganization(2L, "user2", "123456", "user2@example.com", 2L, 2L, "ORG_ADMIN");

        // user1 登录并生成加入自己组织的邀请码
        String token1 = loginAs("user1", "123456");
        Response inviteResponse = authRequest(token1)
                .post("/orgadmin/members/invite-codes/join-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        // user2 登录并使用邀请码加入 user1 的组织
        String token2 = loginAs("user2", "123456");
        Response response = authRequest(token2)
                .body(Map.of("inviteCode", inviteCode))
                .post("/app/user/organizations/join-by-invite");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 切换组织成功
     */
    @Test
    void shouldSwitchOrganization() {
        // 创建用户和两个组织
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        // 创建第二个组织
        createOrganization(2L, "第二个组织");
        // 用户加入第二个组织
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");

        // 登录获取token
        String token = loginAs("testuser", "123456");

        // 切换到第二个组织
        Response response = authRequest(token)
                .body(Map.of("orgId", 2))
                .post("/app/user/switch");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    // ========== 加入组织失败场景 ==========

    /**
     * 已是 ENABLED 成员时通过邀请码加入应失败（覆盖 joinOrganizationByInvite ENABLED 分支）
     */
    @Test
    void shouldFailJoinOrganizationWhenAlreadyEnabledMember() {
        createUserWithOrganization(1L, "user1", "123456", "user1@example.com", 1L, 1L, "ORG_ADMIN");
        String token1 = loginAs("user1", "123456");
        String inviteCode = authRequest(token1)
                .post("/orgadmin/members/invite-codes/join-org")
                .then().statusCode(200)
                .extract().path("data.inviteCode");

        // user2 在独立组织2 中登录，同时已是 org1 的 ENABLED 成员
        createTestUser(2L, "user2", "123456", "user2@example.com");
        createOrganization(2L, "组织2");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 2L);
        createOrganizationMember(2L, 2L, 2L, "ORG_ADMIN");
        jdbcTemplate.update(
                "INSERT INTO `organization_member` (id, org_id, user_id, org_role, status) VALUES (?, ?, ?, ?, 1)",
                3L, 1L, 2L, "MEMBER"
        );
        String token2 = loginAs("user2", "123456");

        Response response = authRequest(token2)
                .body(Map.of("inviteCode", inviteCode))
                .post("/app/user/organizations/join-by-invite");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }

    /**
     * 成员状态已被禁用时通过邀请码加入应失败（覆盖 joinOrganizationByInvite DISABLED 分支）
     */
    @Test
    void shouldFailJoinOrganizationWhenMemberDisabled() {
        createUserWithOrganization(1L, "user1", "123456", "user1@example.com", 1L, 1L, "ORG_ADMIN");
        String token1 = loginAs("user1", "123456");
        String inviteCode = authRequest(token1)
                .post("/orgadmin/members/invite-codes/join-org")
                .then().statusCode(200)
                .extract().path("data.inviteCode");

        // user2 在 org1 中的成员记录状态为 DISABLED（status=0）
        createTestUser(2L, "user2", "123456", "user2@example.com");
        createOrganization(2L, "组织2");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 2L);
        createOrganizationMember(2L, 2L, 2L, "ORG_ADMIN");
        jdbcTemplate.update(
                "INSERT INTO `organization_member` (id, org_id, user_id, org_role, status) VALUES (?, ?, ?, ?, 0)",
                3L, 1L, 2L, "MEMBER"
        );
        String token2 = loginAs("user2", "123456");

        Response response = authRequest(token2)
                .body(Map.of("inviteCode", inviteCode))
                .post("/app/user/organizations/join-by-invite");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }

    /**
     * 已有待审核入组申请时再次通过邀请码加入应失败（覆盖 joinOrganizationByInvite hasPendingApplication 分支）
     */
    @Test
    void shouldFailJoinOrganizationWhenHasPendingApplication() {
        createUserWithOrganization(1L, "user1", "123456", "user1@example.com", 1L, 1L, "ORG_ADMIN");
        String token1 = loginAs("user1", "123456");
        String inviteCode = authRequest(token1)
                .post("/orgadmin/members/invite-codes/join-org")
                .then().statusCode(200)
                .extract().path("data.inviteCode");

        // user2 未加入 org1，但已有 PENDING 申请（status=0, application_type=1 EXISTING_USER）
        createTestUser(2L, "user2", "123456", "user2@example.com");
        createOrganization(2L, "组织2");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 2L);
        createOrganizationMember(2L, 2L, 2L, "ORG_ADMIN");
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, password, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 0, 1, ?, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, "user2", passwordEncoder.encode("123456"), "用户2", "user2@example.com"
        );
        String token2 = loginAs("user2", "123456");

        Response response = authRequest(token2)
                .body(Map.of("inviteCode", inviteCode))
                .post("/app/user/organizations/join-by-invite");

        response.then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }


    /**
     * 使用邮箱验证码重置密码成功
     */
    @Test
    void shouldResetPasswordSuccessfully() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        createEmailVerifyOTP("A1B2C3", 1L, "test@example.com", EmailVerifyBusinessType.FORGOT_PASSWORD);

        Response response = request()
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "new-password"))
                .put("/app/user/password");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        request()
                .body(Map.of("username", "testuser", "password", "123456"))
                .post("/app/session/pwd")
                .then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));

        request()
                .body(Map.of("username", "testuser", "password", "new-password"))
                .post("/app/session/pwd")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue());

        request()
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "another-password"))
                .put("/app/user/password")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /**
     * 验证码错误时重置密码失败
     */
    @Test
    void shouldFailResetPasswordWithWrongCode() {
        createTestUser(1L, "testuser", "123456", "test@example.com");

        request()
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "new-password"))
                .put("/app/user/password")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /**
     * 邮箱不存在时重置密码失败
     */
    @Test
    void shouldFailResetPasswordWithNonexistentEmail() {
        createEmailVerifyOTP("A1B2C3", 1L, "missing@example.com", EmailVerifyBusinessType.FORGOT_PASSWORD);

        request()
                .body(Map.of("email", "missing@example.com", "code", "A1B2C3", "password", "new-password"))
                .put("/app/user/password")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }


    // ========== 认证失败 ==========

    /**
     * 未认证获取用户信息失败
     */
    @Test
    void shouldFailGetMyInfoWithoutAuth() {
        Response response = request()
                .get("/app/user/");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
}

package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.dao.DuplicateKeyException;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.*;

public class UserApiTest extends BaseApiTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void shouldAllowMissingEmailsWithoutRelaxingBoundEmailUniqueness() {
        createTestUser(1L, "legacyone", "123456", null);
        createTestUser(2L, "legacytwo", "123456", null);

        jdbcTemplate.update("UPDATE `user` SET email = ? WHERE id = 1", "bound@example.com");
        assertThatThrownBy(() -> jdbcTemplate.update(
                "UPDATE `user` SET email = ? WHERE id = 2", "BOUND@example.com"))
                .isInstanceOf(DuplicateKeyException.class);
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM `user` WHERE id = 2", String.class)).isNull();

        jdbcTemplate.update("UPDATE `user` SET email = NULL WHERE id = 1");
        jdbcTemplate.update("UPDATE `user` SET email = ? WHERE id = 2", "bound@example.com");
        assertThat(jdbcTemplate.queryForObject("SELECT email FROM `user` WHERE id = 2", String.class))
                .isEqualTo("bound@example.com");
    }

    // ========== 注册与创建组织 ==========

    /**
     * 通过邀请码注册并创建组织成功
     */
    @Test
    void shouldRegisterAndCreateOrganization() {
        // 创建超管并生成邀请码
        String adminToken = createRootAdmin();
        Response inviteResponse = authRequest(adminToken)
                .body(inviteExpiryBody())
                .post("/admin/organizations/invite-codes/create-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        long userId = uniqueId();
        createEmailVerifyOTP("123456", userId, "newuser_" + userId + "@example.com");
        createOrganizationCreateInviteOTP(inviteCode);

        Response response = request()
                .body(Map.of(
                        "email", "newuser_" + userId + "@example.com",
                        "password", "River!82Maple",
                        "username", "New_User",
                        "nickname", "新用户",
                        "inviteCode", inviteCode,
                        "code", "123456",
                        "orgName", "测试组织"
                ))
                .post("/app/user/register/create-organization");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        org.junit.jupiter.api.Assertions.assertEquals("ORG_OWNER", jdbcTemplate.queryForObject(
                "SELECT m.org_role FROM organization_member m JOIN `user` u ON u.id = m.user_id WHERE u.username = ?",
                String.class, "New_User"));
    }

    @Test
    void shouldRejectLoginCodeWhenRegistering() {
        String adminToken = createRootAdmin();
        String inviteCode = authRequest(adminToken)
                .body(inviteExpiryBody())
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
                otpId, "654321", testNow().plusMinutes(30), 3, 0, 0L
        );
        jdbcTemplate.update(
                "INSERT INTO email_verify_otp (id, otp_id, receiver, business_type, send_status) VALUES (?, ?, ?, ?, ?)",
                otpId, otpId, email, 0, 1
        );

        Response response = request()
                .body(Map.of(
                        "email", email,
                        "password", "River!82Maple",
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
                .body(inviteExpiryBody())
                .post("/orgadmin/members/invite-codes/join-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        // 新用户通过邀请码注册并加入组织
        long userId = uniqueId();
        createEmailVerifyOTP("123456", userId, "newuser_" + userId + "@example.com");

        Response createResponse = request()
                .body(Map.of(
                        "email", "newuser_" + userId + "@example.com",
                        "password", "River!82Maple",
                        "username", "New_User",
                        "nickname", "新用户",
                        "inviteCode", inviteCode,
                        "code", "123456"
                ))
                .post("/app/user/register/join-organization");

        createResponse.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"create-organization", "join-organization"})
    void shouldRejectDisallowedUsernameWithoutConsumingCodes(String registrationMode) {
        if ("create-organization".equals(registrationMode)) {
            createOrganizationCreateInviteOTP("INVITE");
        } else {
            createOrganization(1L, "测试组织");
            createOrganizationUserInviteOTP("INVITE", 1L);
        }
        createEmailVerifyOTP("EMAIL1", 1L, "new@example.com");

        for (String username : List.of("用户名称", "１２３４", "ab-c", "ab c", "café", "abcd\n")) {
            request().body(Map.of(
                            "username", username, "password", "River!82Maple", "nickname", "新用户",
                            "email", "new@example.com", "inviteCode", "INVITE", "code", "EMAIL1",
                            "orgName", "新组织"))
                    .post("/app/user/register/" + registrationMode)
                    .then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_time_password WHERE status <> 0", Integer.class)).isZero();

        String validUsername = "create-organization".equals(registrationMode) ? "1234" : "_New_User9";
        request().body(Map.of(
                        "username", validUsername, "password", "River!82Maple", "nickname", "新用户",
                        "email", "new@example.com", "inviteCode", "INVITE", "code", "EMAIL1",
                        "orgName", "新组织"))
                .post("/app/user/register/" + registrationMode)
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        String table = "create-organization".equals(registrationMode) ? "`user`" : "group_application";
        assertThat(jdbcTemplate.queryForObject(
                "SELECT username FROM " + table + " WHERE email = ?", String.class, "new@example.com"))
                .isEqualTo(validUsername);
    }

    /** 缺少大写、小写或数字的密码不得创建账号或申请，失败后仍可使用同一组验证码注册。 */
    @ParameterizedTest
    @ValueSource(strings = {"create-organization", "join-organization"})
    void shouldRejectPasswordsMissingCharacterClassWithoutConsumingRegistrationCodes(String registrationMode) {
        boolean creatingOrganization = "create-organization".equals(registrationMode);
        if (creatingOrganization) {
            createOrganizationCreateInviteOTP("INVITE");
        } else {
            createOrganization(1L, "测试组织");
            createOrganizationUserInviteOTP("INVITE", 1L);
        }
        createEmailVerifyOTP("EMAIL1", 1L, "new@example.com");

        for (String password : List.of("123456", "password1", "PASSWORD1", "Password", "a-password-b", "PaSsWoRd")) {
            request().body(Map.of(
                            "username", "newuser", "password", password, "nickname", "新用户",
                            "email", "new@example.com", "inviteCode", "INVITE", "code", "EMAIL1",
                            "orgName", "新组织"))
                    .post("/app/user/register/" + registrationMode)
                    .then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                    .body("msg", equalTo(PatternConstants.PASSWORD_COMPLEXITY_MESSAGE))
                    .body("data", nullValue());
        }

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM `user`", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization", Integer.class))
                .isEqualTo(creatingOrganization ? 0 : 1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM one_time_password WHERE status <> 0", Integer.class)).isZero();

        String password = "Password1";
        request().body(Map.of(
                        "username", "newuser", "password", password, "nickname", "新用户",
                        "email", "new@example.com", "inviteCode", "INVITE", "code", "EMAIL1",
                        "orgName", "新组织"))
                .post("/app/user/register/" + registrationMode)
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String hash = creatingOrganization
                ? jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE username = ?", String.class, "newuser")
                : jdbcTemplate.queryForObject("SELECT password_hash FROM group_application WHERE username = ?", String.class, "newuser");
        assertThat(passwordEncoder.matches(password, hash)).isTrue();
        assertThat(passwordEncoder.matches("river!82maple", hash)).isFalse();
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
                .body(inviteExpiryBody())
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
        Response response = userRequest(token)
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
        Response response = userRequest(token)
                .get("/app/user/organizations");

        response.then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void shouldResizeHeaderAvatarsOnly() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(11L, 1L, 1L);
        createResource(12L, 1L, 1L);
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = 'avatar', storage_key = 'avatar/user.png' WHERE id = 11");
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = 'avatar', storage_key = 'avatar/org.png' WHERE id = 12");
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 11 WHERE id = 1");
        jdbcTemplate.update("UPDATE organization SET avatar_file_id = 12 WHERE id = 1");
        String token = loginAs("testuser", "123456");

        userRequest(token).get("/app/user/").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.avatarUrl", allOf(
                        containsString("key=avatar/user.png"),
                        containsString("&eo-img.resize=w/64/h/64")));
        userRequest(token).get("/app/user/organizations").then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data[0].avatarUrl", allOf(
                        containsString("key=avatar/org.png"),
                        containsString("&eo-img.resize=w/64/h/64")));
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
        Response inviteResponse = authRequest(token1, 1L)
                .body(inviteExpiryBody())
                .post("/orgadmin/members/invite-codes/join-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String inviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        // user2 登录并使用邀请码加入 user1 的组织
        String token2 = loginAs("user2", "123456");
        Response response = userRequest(token2)
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
        Response response = userRequest(token)
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
                .body(inviteExpiryBody())
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
                3L, 1L, 2L, "ORG_USER"
        );
        String token2 = loginAs("user2", "123456");

        Response response = userRequest(token2)
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
                .body(inviteExpiryBody())
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
                3L, 1L, 2L, "ORG_USER"
        );
        String token2 = loginAs("user2", "123456");

        Response response = userRequest(token2)
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
                .body(inviteExpiryBody())
                .post("/orgadmin/members/invite-codes/join-org")
                .then().statusCode(200)
                .extract().path("data.inviteCode");

        // user2 未加入 org1，但已有 PENDING 申请（status=0, application_type=1 EXISTING_USER）
        createTestUser(2L, "user2", "123456", "user2@example.com");
        createOrganization(2L, "组织2");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 2L);
        createOrganizationMember(2L, 2L, 2L, "ORG_ADMIN");
        jdbcTemplate.update(
                "INSERT INTO `group_application` (id, org_id, user_id, status, application_type, username, nickname, email, created_at, updated_at) " +
                        "VALUES (?, ?, ?, 0, 1, ?, ?, ?, NOW(), NOW())",
                1L, 1L, 2L, "user2", "用户2", "user2@example.com"
        );
        String token2 = loginAs("user2", "123456");

        Response response = userRequest(token2)
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
        String token = loginAs("testuser", "123456");
        createEmailVerifyOTP("A1B2C3", 1L, "test@example.com", EmailVerifyBusinessType.FORGOT_PASSWORD);

        Response response = request()
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "New-password1"))
                .put("/app/user/password");

        response.then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        request()
                .body(Map.of("username", "testuser", "password", "123456"))
                .post("/app/session/pwd")
                .then()
                .statusCode(200)
                .body("code", not(equalTo(ResultCode.SUCCESS.getCode())));

        request()
                .body(Map.of("username", "testuser", "password", "New-password1"))
                .post("/app/session/pwd")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue());

        request()
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "Another-password1"))
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
                .body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "New-password1"))
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
                .body(Map.of("email", "missing@example.com", "code", "A1B2C3", "password", "New-password1"))
                .put("/app/user/password")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }


    @Test
    void shouldChangePasswordWithCurrentPassword() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().body(Map.of("username", "testuser", "password", "River!82Maple")).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        request().body(Map.of("username", "testuser", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200).body("code", not(equalTo(ResultCode.SUCCESS.getCode())));
    }

    @Test
    void shouldRejectPasswordChangeWhenUserRecordIsGone() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");
        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", 1L);

        userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }

    @Test
    void shouldRejectRequestWhenLoginCacheMissesAndUserRowIsGone() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");
        redisTemplate.delete(RedisConstants.LOGIN_USER + 1L);
        jdbcTemplate.update("DELETE FROM `user` WHERE id = ?", 1L);

        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }

    @Test
    void shouldRejectPasswordChangeForWrongOldPasswordMissingAuthAndInvalidLength() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        userRequest(token).body(Map.of("oldPassword", "wrong1", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,原密码错误"));
        request().body(Map.of("oldPassword", "123456", "newPassword", "River!82Maple"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", "123"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 找回密码拒绝缺少大写、小写或数字的密码时保留原密码、会话和验证码，同一验证码仍可设置合格密码。 */
    @Test
    void shouldRejectResetPasswordsMissingCharacterClassWithoutChangingCredentialsOrConsumingCode() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");
        String originalHash = jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE id = 1", String.class);
        createEmailVerifyOTP("A1B2C3", 1L, "test@example.com", EmailVerifyBusinessType.FORGOT_PASSWORD);

        for (String password : List.of("123456", "password1", "PASSWORD1", "Password", "a-password-b")) {
            request().body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", password))
                    .put("/app/user/password")
                    .then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                    .body("msg", equalTo(PatternConstants.PASSWORD_COMPLEXITY_MESSAGE))
                    .body("data", nullValue());
        }

        assertThat(jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE id = 1", String.class))
                .isEqualTo(originalHash);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code = ?",
                Integer.class, "A1B2C3")).isZero();
        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        request().body(Map.of("email", "test@example.com", "code", "A1B2C3", "password", "Password1"))
                .put("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(passwordEncoder.matches("Password1", jdbcTemplate.queryForObject(
                "SELECT password FROM `user` WHERE id = 1", String.class))).isTrue();
    }

    /** 修改密码拒绝缺少字符类别的新密码时保留原密码和会话，已有弱旧密码仍可用于验证身份。 */
    @Test
    void shouldRejectNewPasswordsMissingCharacterClassWithoutChangingCredentialsOrRevokingSession() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");
        String originalHash = jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE id = 1", String.class);

        for (String password : List.of("123456", "password1", "PASSWORD1", "Password", "a-password-b")) {
            userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", password))
                    .patch("/app/user/password")
                    .then().statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                    .body("msg", equalTo(PatternConstants.PASSWORD_COMPLEXITY_MESSAGE))
                    .body("data", nullValue());
        }

        assertThat(jdbcTemplate.queryForObject("SELECT password FROM `user` WHERE id = 1", String.class))
                .isEqualTo(originalHash);
        userRequest(token).get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", "Password1"))
                .patch("/app/user/password")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(passwordEncoder.matches("Password1", jdbcTemplate.queryForObject(
                "SELECT password FROM `user` WHERE id = 1", String.class))).isTrue();
    }

    /** 只改昵称时保留已有头像，20 个字符的昵称可以保存。 */
    @Test
    void shouldUpdateNicknameAtMaxLengthAndKeepAvatar() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_USER");
        createResource(11L, 1L, 1L);
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 11, nickname = '旧昵称' WHERE id = 1");
        jdbcTemplate.update("UPDATE resource SET biz_id = 1 WHERE id = 11");
        String token = loginAs("testuser", "123456");
        String nickname = "名".repeat(20);

        userRequest(token).body(Map.of("nickname", nickname))
                .put("/app/user/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT nickname FROM `user` WHERE id = 1", String.class))
                .isEqualTo(nickname);
        assertThat(jdbcTemplate.queryForObject("SELECT avatar_file_id FROM `user` WHERE id = 1", Long.class))
                .isEqualTo(11L);
        assertThat(jdbcTemplate.queryForObject("SELECT process_status FROM resource WHERE id = 11", Integer.class))
                .isEqualTo(FileProcessStatus.Activated.getCode());
        userRequest(token).get("/app/user/")
                .then()
                .statusCode(200)
                .body("data.nickname", equalTo(nickname));
    }

    /** 空白、超长或缺失昵称不改资料；未登录不能修改。 */
    @Test
    void shouldRejectInvalidNicknameWithoutChangingProfile() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_USER");
        createResource(11L, 1L, 1L);
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 11 WHERE id = 1");
        String token = loginAs("testuser", "123456");

        for (Object nickname : List.of("   ", "", "名".repeat(21))) {
            userRequest(token).body(Map.of("nickname", nickname))
                    .put("/app/user/")
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        }
        userRequest(token).body(Map.of())
                .put("/app/user/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        request().body(Map.of("nickname", "新昵称"))
                .put("/app/user/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT nickname FROM `user` WHERE id = 1", String.class))
                .isEqualTo("testuser");
        assertThat(jdbcTemplate.queryForObject("SELECT avatar_file_id FROM `user` WHERE id = 1", Long.class))
                .isEqualTo(11L);
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

    @Test
    void shouldBindEmailAndKeepCurrentSession() {
        createTestUser(1L, "legacyone", "123456", null);
        String token = loginAs("legacyone", "123456");
        createEmailVerifyOTP("A1B2C3", 1L, "bind@example.com", EmailVerifyBusinessType.BIND_EMAIL);

        userRequest(token)
                .body(Map.of("email", "bind@example.com", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", equalTo("邮箱已绑定"));

        userRequest(token)
                .get("/app/user/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.email", equalTo("bind@example.com"));

        createEmailVerifyOTP("D4E5F6", 1L, "bind@example.com", EmailVerifyBusinessType.LOGIN);
        request()
                .body(Map.of("email", "bind@example.com", "code", "D4E5F6"))
                .post("/app/session/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.token", notNullValue());
    }

    @Test
    void shouldRejectBindEmailWhenUserAlreadyHasEmail() {
        createTestUser(1L, "legacyone", "123456", "bound@example.com");
        String token = loginAs("legacyone", "123456");
        createEmailVerifyOTP("A1B2C3", 1L, "other@example.com", EmailVerifyBusinessType.BIND_EMAIL);

        userRequest(token)
                .body(Map.of("email", "other@example.com", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,邮箱已绑定"));

        assertThat(jdbcTemplate.queryForObject("SELECT email FROM `user` WHERE id = 1", String.class))
                .isEqualTo("bound@example.com");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM one_time_password WHERE code = ?", Integer.class, "A1B2C3")).isZero();
    }

    @Test
    void shouldRejectBindEmailWhenAddressIsTakenWithoutConsumingCode() {
        createTestUser(1L, "legacyone", "123456", null);
        createTestUser(2L, "legacytwo", "123456", "taken@example.com");
        String token = loginAs("legacyone", "123456");
        createEmailVerifyOTP("A1B2C3", 1L, "taken@example.com", EmailVerifyBusinessType.BIND_EMAIL);

        userRequest(token)
                .body(Map.of("email", "taken@example.com", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,邮箱已存在"));

        assertThat(jdbcTemplate.queryForObject("SELECT email FROM `user` WHERE id = 1", String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM one_time_password WHERE code = ?", Integer.class, "A1B2C3")).isZero();
    }

    @Test
    void shouldRejectBindEmailWhenCodeIsForLogin() {
        createTestUser(1L, "legacyone", "123456", null);
        String token = loginAs("legacyone", "123456");
        createEmailVerifyOTP("A1B2C3", 1L, "bind@example.com", EmailVerifyBusinessType.LOGIN);

        userRequest(token)
                .body(Map.of("email", "bind@example.com", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT email FROM `user` WHERE id = 1", String.class)).isNull();
    }

    @Test
    void shouldRejectBindEmailWithoutAuthOrWithInvalidFields() {
        createTestUser(1L, "legacyone", "123456", null);
        String token = loginAs("legacyone", "123456");

        request()
                .body(Map.of("email", "bind@example.com", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        userRequest(token)
                .body(Map.of("email", "", "code", "A1B2C3"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        userRequest(token)
                .body(Map.of("email", "bind@example.com", "code", "abc"))
                .put("/app/user/email")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectBindEmailCodeWhenEmailIsAlreadyUsed() {
        createTestUser(1L, "legacyone", "123456", "bound@example.com");
        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verify_otp", Integer.class);

        request()
                .body(Map.of("email", "bound@example.com", "businessType", "BIND_EMAIL"))
                .post("/app/session/email/code")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("邮箱已存在"));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM email_verify_otp", Integer.class))
                .isEqualTo(before);
    }
}

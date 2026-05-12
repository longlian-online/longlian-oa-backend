package online.longlian.app.api.app.user;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

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

    /**
     * 通过邀请码注册并加入组织成功
     */
    @Test
    void shouldRegisterAndJoinOrganization() {
        // 创建超管并生成邀请码
        String adminToken = createRootAdmin();
        Response inviteResponse = authRequest(adminToken)
                .post("/admin/organizations/invite-codes/create-org");
        inviteResponse.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String adminInviteCode = inviteResponse.jsonPath().getString("data.inviteCode");

        long userId = uniqueId();
        createEmailVerifyOTP("123456", userId, "orgadmin_" + userId + "@example.com");
        createOrganizationUserInviteOTP(adminInviteCode, 0L);

        Response createResponse = request()
                .body(Map.of(
                        "email", "orgadmin_" + userId + "@example.com",
                        "password", "123456",
                        "username", "orgadmin_" + userId,
                        "nickname", "组织管理员",
                        "inviteCode", adminInviteCode,
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

    // ========== 认证失败 ==========

    /**
     * 未认证获取用户信息失败
     */
    @Test
    void shouldFailGetMyInfoWithoutAuth() {
        Response response = request()
                .get("/app/user/");

        response.then()
                .statusCode(401);
    }
}
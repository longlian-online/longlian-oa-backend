package online.longlian.app.api;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import online.longlian.app.api.util.DatabaseCleanupUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDateTime;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

/**
 * API 测试基类。
 * 每个测试方法前自动清空测试数据库，保证用例间完全隔离。
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseApiTest {

    @LocalServerPort
    protected int port;

    @Autowired
    private DatabaseCleanupUtil databaseCleanupUtil;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected PasswordEncoder passwordEncoder;

    @BeforeAll
    void baseBeforeAll() {
        databaseCleanupUtil.initSchema();
    }

    @BeforeEach
    void baseSetUp() {
        RestAssured.port = port;
        RestAssured.baseURI = "http://localhost";
        databaseCleanupUtil.truncateAllTables();
    }

    /**
     * 登录并返回 JWT token
     */
    protected String loginAs(String username, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/app/session/pwd");

        if (response.statusCode() != 200 || response.jsonPath().getInt("code") != 0) {
            throw new RuntimeException("用户端登录失败: " + response.jsonPath().getString("msg"));
        }

        return response.jsonPath().getString("data.token");
    }

    /**
     * 登录管理端并返回 JWT token
     */
    protected String adminLoginAs(String username, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/admin/session");
        if (response.statusCode() != 200 || response.jsonPath().getInt("code") != 0) {
            throw new RuntimeException("管理端登录失败: " + response.jsonPath().getString("msg"));
        }

        return response.jsonPath().getString("data.token");
    }

    /**
     * 直接向数据库插入测试用户（供测试前置数据使用）
     */
    protected void createTestUser(long userId, String username, String password, String email) {
        jdbcTemplate.update(
                "INSERT INTO `user` (id, username, password, nickname, email) VALUES (?, ?, ?, ?, ?)",
                userId, username, passwordEncoder.encode(password), username, email
        );
    }

    /**
     * 直接向数据库插入测试管理员（供测试前置数据使用）
     */
    protected void createAdmin(long id, String username, String password, String role) {
        jdbcTemplate.update(
                "INSERT INTO `admin` (id, username, password, role) VALUES (?, ?, ?, ?)",
                id, username, passwordEncoder.encode(password), role
        );
    }

    /**
     * 创建组织
     */
    protected void createOrganization(long orgId, String name) {
        jdbcTemplate.update(
                "INSERT INTO `organization` (id, name, creator_id, status) VALUES (?, ?, 0, 1)",
                orgId, name
        );
    }

    /**
     * 创建组织成员关联
     */
    protected void createOrganizationMember(long id, long orgId, long userId, String orgRole) {
        jdbcTemplate.update(
                "INSERT INTO `organization_member` (id, org_id, user_id, org_role, status) VALUES (?, ?, ?, ?, 1)",
                id, orgId, userId, orgRole
        );
    }

    /**
     * 一次性创建用户及其组织（包含：组织 + 用户（设置默认组织）+ 组织成员）
     */
    protected void createUserWithOrganization(long userId, String username, String password,
                                               String email,
                                               long orgMemberId, long orgId, String orgRole) {
        createOrganization(orgId, "组织" + orgId);
        createTestUser(userId, username, password, email);
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", orgId, userId);
        createOrganizationMember(orgMemberId, orgId, userId, orgRole);
    }

    /**
     * 创建带默认 Content-Type 的请求规范
     */
    protected RequestSpecification request() {
        return given().contentType(ContentType.JSON);
    }

    /**
     * 创建带 Bearer Token 认证头的请求规范
     */
    protected RequestSpecification authRequest(String token) {
        return given()
                .contentType(ContentType.JSON)
                .header("Authorization", "Bearer " + token);
    }

    protected String createRootAdmin() {
        long id = System.currentTimeMillis();
        createAdmin(id, "root_" + id, "123456", "root");
        return adminLoginAs("root_" + id, "123456");
    }

    protected String createNormalAdmin() {
        long id = System.currentTimeMillis();
        createAdmin(id, "admin_" + id, "123456", "ADMIN");
        return adminLoginAs("admin_" + id, "123456");
    }

    /**
     * 创建一次性密码（OTP）记录
     */
    protected void createOTP(String code, Integer bizType, Long creatorId, LocalDateTime expiredAt) {
        jdbcTemplate.update(
                "INSERT INTO `one_time_password` (id, code, expired_at, biz_type, status, creator_id) VALUES (?, ?, ?, ?, 0, ?)",
                System.nanoTime(), code, expiredAt, bizType, creatorId
        );
    }

    /**
     * 创建邮箱验证码 OTP（默认 30 分钟后过期）
     */
    protected void createEmailVerifyOTP(String code, Long creatorId) {
        createOTP(code, 3, creatorId, LocalDateTime.now().plusMinutes(30));
    }

    /**
     * 创建邀请加入组织 OTP（默认 30 分钟后过期）
     */
    protected void createOrganizationUserInviteOTP(String code, Long orgId) {
        long otpId = System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO `one_time_password` (id, code, expired_at, biz_type, status, creator_id) VALUES (?, ?, ?, ?, 0, 0)",
                otpId, code, LocalDateTime.now().plusMinutes(30), 2
        );
        jdbcTemplate.update(
                "INSERT INTO `organization_join_otp` (id, otp_id, org_id) VALUES (?, ?, ?)",
                otpId, otpId, orgId
        );
    }

    /**
     * 创建邀请创建组织 OTP（默认 30 分钟后过期）
     */
    protected void createOrganizationCreateInviteOTP(String code) {
        long otpId = System.nanoTime();
        jdbcTemplate.update(
                "INSERT INTO `one_time_password` (id, code, expired_at, biz_type, status, creator_id) VALUES (?, ?, ?, ?, 0, 0)",
                otpId, code, LocalDateTime.now().plusMinutes(30), 1
        );
        jdbcTemplate.update(
                "INSERT INTO `organization_create_otp` (id, otp_id) VALUES (?, ?)",
                otpId, otpId
        );
    }

    protected void assertErrorCode(Response response, int expectedCode) {
        response.then().body("code", equalTo(expectedCode));
    }
}

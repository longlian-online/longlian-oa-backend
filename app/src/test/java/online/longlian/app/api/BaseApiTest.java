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

import java.util.Map;

import static io.restassured.RestAssured.given;

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

        return response.jsonPath().getString("data.token");
    }

    /**
     * 登录管理端并返回 JWT token
     */
    protected String adminLoginAs(String username, String password) {
        Response response = given()
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post("/admin/session/login");

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
}

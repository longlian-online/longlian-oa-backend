package online.longlian.app.api.session;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class SessionApiTest extends BaseApiTest {

    // ========== 密码登录 ==========

    /**
     * 用户端密码登录成功
     */
    @Test
    void shouldLoginSuccessfully() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");

        Response response = request()
                .body(Map.of("username", "testuser", "password", "123456"))
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue())
                .body("data.userId", notNullValue());
    }

    /**
     * 密码错误登录失败
     */
    @Test
    void shouldFailWithWrongPassword() {
        createTestUser(1L, "testuser", "123456", "test@example.com");

        Response response = request()
                .body(Map.of("username", "testuser", "password", "wrongpassword"))
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 用户名不存在登录失败
     */
    @Test
    void shouldFailWithNonexistentUser() {
        Response response = request()
                .body(Map.of("username", "nonexistent", "password", "123456"))
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }

    // ========== 验证码登录 ==========

    /**
     * 通过邮箱验证码登录成功
     */
    @Test
    void shouldLoginByCodeSuccessfully() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");

        createEmailVerifyOTP("123456", 1L, "test@example.com", EmailVerifyBusinessType.LOGIN);

        Response response = request()
                .body(Map.of("email", "test@example.com", "code", "123456"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.token", notNullValue());
    }

    /**
     * 验证码错误时登录失败
     */
    @Test
    void shouldFailLoginByCodeWithWrongCode() {
        createTestUser(1L, "testuser", "123456", "test@example.com");

        Response response = request()
                .body(Map.of("email", "test@example.com", "code", "000000"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 邮箱不存在时验证码登录失败
     */
    @Test
    void shouldFailLoginByCodeWithNonexistentEmail() {
        Response response = request()
                .body(Map.of("email", "nonexistent@example.com", "code", "123456"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    /**
     * 邮箱格式不正确时验证码登录失败
     */
    @Test
    void shouldFailLoginByCodeWithInvalidEmail() {
        Response response = request()
                .body(Map.of("email", "invalid-email", "code", "123456"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 邮箱为空时验证码登录失败
     */
    @Test
    void shouldFailLoginByCodeWithEmptyEmail() {
        Response response = request()
                .body(Map.of("email", "", "code", "123456"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 验证码为空时登录失败
     */
    @Test
    void shouldFailLoginByCodeWithEmptyCode() {
        Response response = request()
                .body(Map.of("email", "test@example.com", "code", ""))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 验证码长度不为6位时登录失败
     */
    @Test
    void shouldFailLoginByCodeWithShortCode() {
        Response response = request()
                .body(Map.of("email", "test@example.com", "code", "12345"))
                .post("/app/session/email");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 发送邮箱验证码 ==========

    /**
     * 发送邮箱验证码成功
     */
    @Test
    void shouldSendEmailCodeSuccessfully() {
        Response response = request()
                .body(Map.of("email", "test@example.com", "businessType", "LOGIN"))
                .post("/app/session/email/code");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 邮箱为空时发送验证码失败
     */
    @Test
    void shouldFailSendCodeWithEmptyEmail() {
        Response response = request()
                .body(Map.of("email", "", "businessType", "LOGIN"))
                .post("/app/session/email/code");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 邮箱格式不正确时发送验证码失败
     */
    @Test
    void shouldFailSendCodeWithInvalidEmail() {
        Response response = request()
                .body(Map.of("email", "not-an-email", "businessType", "LOGIN"))
                .post("/app/session/email/code");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 缺少业务类型时发送验证码失败
     */
    @Test
    void shouldFailSendCodeWithNullBusinessType() {
        Response response = request()
                .body(Map.of("email", "test@example.com"))
                .post("/app/session/email/code");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    // ========== 退出登录 ==========

    /**
     * 登出成功
     */
    @Test
    void shouldLogoutSuccessfully() {
        createUserWithOrganization(1L, "testuser", "123456", "test@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("testuser", "123456");

        Response response = authRequest(token)
                .delete("/app/session/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    /**
     * 使用黑名单 Token 访问接口失败（已登出）
     */
    @Test
    void shouldFailWithBlacklistedToken() {
        long uid = uniqueId();
        long oid = uniqueId();
        createUserWithOrganization(uid, "blacklistuser", "123456", "blacklist@example.com", uid, oid, "ORG_ADMIN");
        String token = loginAs("blacklistuser", "123456");

        authRequest(token).delete("/app/session/");

        Response response = authRequest(token).get("/app/user/");
        response.then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 使用无效 Token 访问受保护资源失败
     */
    @Test
    void shouldFailWithInvalidToken() {
        Response response = authRequest("invalid.token.here")
                .get("/app/user/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 无 Token 退出登录被 Spring Security 拦截
     */
    @Test
    void shouldLogoutWithoutAuthHeader() {
        Response response = request()
                .delete("/app/session/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
}

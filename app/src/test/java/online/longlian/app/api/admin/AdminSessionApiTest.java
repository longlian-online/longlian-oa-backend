package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class AdminSessionApiTest extends BaseApiTest {

    @Test
    void shouldLoginSuccessfully() {
        createAdmin(1L, "admin", "123456", "SUPER_ADMIN");

        Response response = request()
                .body(Map.of("username", "admin", "password", "123456"))
                .post("/admin/session");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", notNullValue())
                .body("data.token", notNullValue())
                .body("data.adminId", notNullValue());
    }

    @Test
    void shouldFailWithWrongPassword() {
        createAdmin(2L, "admin2", "123456", "SUPER_ADMIN");

        Response response = request()
                .body(Map.of("username", "admin2", "password", "wrongpassword"))
                .post("/admin/session");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithNonexistentAdmin() {
        Response response = request()
                .body(Map.of("username", "nonexistent", "password", "123456"))
                .post("/admin/session");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.USER_NOT_EXIT.getCode()));
    }

    @Test
    void shouldLogoutSuccessfully() {
        createAdmin(3L, "admin3", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("admin3", "123456");

        Response response = authRequest(token)
                .delete("/admin/session");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0));
    }

    @Test
    void shouldFailWithBlacklistedToken() {
        createAdmin(4L, "admin4", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("admin4", "123456");

        authRequest(token).delete("/admin/session");

        Response response = authRequest(token).get("/admin/admins/");
        response.then().statusCode(401);
    }

    @Test
    void shouldFailWithInvalidToken() {
        Response response = authRequest("invalid.token.here")
                .get("/admin/admins/");
        response.then().statusCode(401);
    }

    @Test
    void shouldFailWithEmptyUsername() {
        Response response = request()
                .body(Map.of("username", "", "password", "123456"))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithEmptyPassword() {
        Response response = request()
                .body(Map.of("username", "admin", "password", ""))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithMissingUsername() {
        Response response = request()
                .body(Map.of("password", "123456"))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithMissingPassword() {
        Response response = request()
                .body(Map.of("username", "admin"))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithTooLongUsername() {
        Response response = request()
                .body(Map.of("username", "a".repeat(33), "password", "123456"))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailWithTooShortPassword() {
        Response response = request()
                .body(Map.of("username", "admin", "password", "12345"))
                .post("/admin/session");
        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldLogoutSucceedWithoutAuthHeader() {
        Response response = request().delete("/admin/session");
        response.then().statusCode(401);
    }

    @Test
    void shouldLogoutFailedWithInvalidToken() {
        Response response = authRequest("invalid.token").delete("/admin/session");
        response.then().statusCode(401);
    }
}

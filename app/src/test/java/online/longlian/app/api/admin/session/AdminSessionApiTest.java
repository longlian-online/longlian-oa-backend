package online.longlian.app.api.admin.session;

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
                .body("code", not(equalTo(0)));
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
                .body("code", equalTo(0))
                .body("msg", equalTo("登出成功"));
    }
}

package online.longlian.app.api.session;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

public class SessionApiTest extends BaseApiTest {

    @Test
    void shouldLoginSuccessfully() {
        createTestUser(1L, "testuser", "123456", "test@example.com");

        Response response = request()
                .body("{\"username\":\"testuser\",\"password\":\"123456\"}")
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", notNullValue())
                .body("data.token", notNullValue())
                .body("data.userId", notNullValue());
    }

    @Test
    void shouldFailWithWrongPassword() {
        createTestUser(2L, "testuser2", "123456", "test2@example.com");

        Response response = request()
                .body("{\"username\":\"testuser2\",\"password\":\"wrongpassword\"}")
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    @Test
    void shouldFailWithNonexistentUser() {
        Response response = request()
                .body("{\"username\":\"nonexistent\",\"password\":\"123456\"}")
                .post("/app/session/pwd");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }
}
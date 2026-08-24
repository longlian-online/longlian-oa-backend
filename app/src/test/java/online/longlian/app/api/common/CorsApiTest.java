package online.longlian.app.api.common;

import io.restassured.http.ContentType;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * CORS 跨域 API 测试：验证预检与实际请求的 CORS 响应头，以及未授权来源被拒绝。
 */
class CorsApiTest extends BaseApiTest {

    private static final String ALLOWED_ORIGIN = "http://localhost:3000";
    private static final String DISALLOWED_ORIGIN = "http://evil.example.com";

    @Test
    void shouldRespondToPreflightWithCorsHeadersForAllowedOrigin() {
        given()
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "content-type,authorization")
                .options("/app/session/pwd")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Allow-Methods", containsString("POST"));
    }

    @Test
    void shouldRejectPreflightFromDisallowedOrigin() {
        given()
                .header("Origin", DISALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST")
                .options("/app/session/pwd")
                .then()
                .statusCode(403);
    }

    @Test
    void shouldAttachCorsHeaderToActualRequestFromAllowedOrigin() {
        given()
                .header("Origin", ALLOWED_ORIGIN)
                .contentType(ContentType.JSON)
                .body("{}")
                .post("/app/session/pwd")
                .then()
                .statusCode(200)
                .header("Access-Control-Allow-Origin", ALLOWED_ORIGIN);
    }
}

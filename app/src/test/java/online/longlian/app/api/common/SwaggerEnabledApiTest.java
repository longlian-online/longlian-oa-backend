package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.startsWith;

@TestPropertySource(properties = "SPRINGDOC_ENABLED=true")
class SwaggerEnabledApiTest extends BaseApiTest {
    /** 显式启用时必须保留开发环境使用的 OpenAPI 接口。 */
    @Test
    void shouldExposeDocumentationWhenEnabled() {
        request().get("/v3/api-docs").then().statusCode(200)
                .body("openapi", startsWith("3."));
    }
}

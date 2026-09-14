package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.equalTo;

@TestPropertySource(properties = "SPRINGDOC_ENABLED=false")
class SwaggerDisabledApiTest extends BaseApiTest {
    /** The default application must not expose anonymous documentation. */
    @Test
    void shouldNotExposeDocumentationByDefault() {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui.html"}) {
            request().get(path).then().statusCode(200)
                    .body("code", equalTo(ResultCode.NOT_FOUND.getCode()));
        }
    }
}

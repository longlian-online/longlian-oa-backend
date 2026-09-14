package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.hamcrest.Matchers.equalTo;

@TestPropertySource(properties = "SPRINGDOC_ENABLED=false")
class SwaggerDisabledApiTest extends BaseApiTest {
    /** 默认配置不能匿名暴露接口文档。 */
    @Test
    void shouldNotExposeDocumentationByDefault() {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui.html"}) {
            request().get(path).then().statusCode(200)
                    .body("code", equalTo(ResultCode.NOT_FOUND.getCode()));
        }
    }
}

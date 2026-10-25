package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;

class SwaggerDisabledApiTest extends BaseApiTest {
    /**
     * 默认关闭 springdoc 时，文档路径不在免鉴权白名单；无 handler 时入口返回 NOT_FOUND，
     * 不能匿名拿到 OpenAPI 内容。
     */
    @Test
    void shouldNotExposeDocumentationByDefault() {
        for (String path : new String[]{"/v3/api-docs", "/swagger-ui.html"}) {
            request().get(path).then().statusCode(200)
                    .body("code", equalTo(ResultCode.NOT_FOUND.getCode()));
        }
    }
}

package online.longlian.app.api.common;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;

import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;

class SystemInfoApiTest extends BaseApiTest {

    @Autowired
    private BuildProperties buildProperties;

    /** 公开接口无需 Token、组织或请求参数，且只公开版本信息。 */
    @Test
    void shouldReturnOnlyBuildVersionWithoutAuthentication() {
        request().get("/common/system/info").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("msg", instanceOf(String.class))
                .body("data", equalTo(Map.of("version", buildProperties.getVersion())));
    }

    /** 客户端携带失效凭证也不应阻止读取公开的系统信息。 */
    @Test
    void shouldReturnBuildVersionWithInvalidToken() {
        authRequest("invalid-token").get("/common/system/info").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", equalTo(Map.of("version", buildProperties.getVersion())));
    }

    /** 系统信息不依赖组织上下文，非法组织头也不能阻止匿名读取。 */
    @Test
    void shouldReturnBuildVersionWithInvalidOrganizationHeader() {
        request().header("X-Org-Id", "invalid-org").get("/common/system/info").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", equalTo(Map.of("version", buildProperties.getVersion())));
    }

    /** 白名单仅开放读取，其他 HTTP 方法仍需认证。 */
    @Test
    void shouldNotPermitAnonymousWriteToSystemInfo() {
        request().post("/common/system/info").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /** 新增公开接口不能使相邻路径同时免鉴权。 */
    @Test
    void shouldNotPermitAnonymousAccessToOtherSystemPaths() {
        request().get("/common/system/private").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
}

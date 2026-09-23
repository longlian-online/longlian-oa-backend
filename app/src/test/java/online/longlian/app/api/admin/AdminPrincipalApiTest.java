package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.AdminAuthenticationStrategy;
import online.longlian.app.common.security.AdminUserDetails;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.security.core.Authentication;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

class AdminPrincipalApiTest extends BaseApiTest {
    @SpyBean
    private AdminAuthenticationStrategy strategy;

    /** 登录和已认证的管理操作不应在认证主体中携带密码。 */
    @Test
    void shouldAuthorizeAdminWithCredentialFreePrincipal() {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        AtomicReference<Authentication> captured = new AtomicReference<>();
        doAnswer(invocation -> {
            Authentication authentication = (Authentication) invocation.callRealMethod();
            captured.set(authentication);
            return authentication;
        }).when(strategy).authenticate(1L, anyString());

        authRequest(token).get("/admin/admins/").then()
                .statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].username", equalTo("admin"));
        assertThat(captured.get()).isNotNull();
        assertThat(((AdminUserDetails) captured.get().getPrincipal()).getPassword()).isNull();
        assertThat(captured.get().getCredentials()).isNull();
    }
}

package online.longlian.app.api.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class CaptchaApiTest extends BaseApiTest {
    private static final String SECRET = "captcha-api-private-verification-credential";
    private static final Set<String> SPENT = ConcurrentHashMap.newKeySet();
    private static final AtomicInteger CALLS = new AtomicInteger();
    private static final HttpServer VERIFIER = startVerifier();

    @DynamicPropertySource
    static void captchaConfiguration(DynamicPropertyRegistry registry) {
        registry.add("longlian.captcha.enabled", () -> true);
        registry.add("longlian.captcha.server-url", () -> "https://captcha.public.test");
        registry.add("longlian.captcha.site-key", () -> "oa-test");
        registry.add("longlian.captcha.verify-url",
                () -> "http://127.0.0.1:" + VERIFIER.getAddress().getPort() + "/siteverify");
        registry.add("longlian.captcha.verify-secret", () -> SECRET);
        registry.add("longlian.captcha.allowed-hostnames", () -> "oa.test.longlian.online");
    }

    @BeforeEach
    void resetVerifier() {
        SPENT.clear();
        CALLS.set(0);
    }

    @AfterAll
    void stopVerifier() {
        VERIFIER.stop(0);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/app/session/pwd", "/app/session/email", "/admin/session"})
    void shouldRequireTicketOnEveryLoginEndpoint(String endpoint) {
        request().body(credentials()).post(endpoint).then().statusCode(200)
                .body("code", equalTo(ResultCode.CAPTCHA_REQUIRED.getCode()));
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void shouldAllowPasswordLoginAndRejectTicketReplay() {
        createTestUser(1L, "captcha_user", "123456", "captcha@test.example");
        request().header("X-Captcha-Token", "pwd-one").body(credentials()).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.userId", equalTo("1")).body("data.token", notNullValue());
        request().header("X-Captcha-Token", "pwd-one").body(credentials()).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.CAPTCHA_INVALID.getCode()));
    }

    @Test
    void shouldAllowAdminLoginWithItsOwnScene() {
        createAdmin(1L, "captcha_user", "123456", "root");
        request().header("X-Captcha-Token", "admin-one").body(credentials()).post("/admin/session")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.username", equalTo("captcha_user"));
    }

    @Test
    void shouldPreserveEmailCodeAfterFailedHumanVerification() {
        createTestUser(1L, "captcha_user", "123456", "captcha@test.example");
        createEmailVerifyOTP("ABC123", 0L, "captcha@test.example", EmailVerifyBusinessType.LOGIN);
        Map<String, String> email = Map.of("email", "captcha@test.example", "code", "ABC123");
        request().header("X-Captcha-Token", "invalid").body(email).post("/app/session/email")
                .then().body("code", equalTo(ResultCode.CAPTCHA_INVALID.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT use_count FROM one_time_password WHERE code = 'ABC123'",
                Integer.class)).isZero();
        request().header("X-Captcha-Token", "email-one").body(email).post("/app/session/email")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.userId", equalTo("1"));
        assertThat(jdbcTemplate.queryForObject("SELECT use_count FROM one_time_password WHERE code = 'ABC123'",
                Integer.class)).isEqualTo(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid", "expired", "wrong-host", "admin-wrong-scene"})
    void shouldRejectInvalidTicketsBeforeCheckingCredentials(String token) {
        request().header("X-Captcha-Token", token).body(credentials()).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.CAPTCHA_INVALID.getCode()));
    }

    @Test
    void shouldDenyUnavailableVerificationWithoutExecutingLogin() {
        request().header("X-Captcha-Token", "unavailable").body(credentials()).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.CAPTCHA_UNAVAILABLE.getCode()));
    }

    @Test
    void shouldReturnOriginalCredentialFailureAfterValidVerification() {
        createTestUser(1L, "captcha_user", "123456", "captcha@test.example");
        request().header("X-Captcha-Token", "pwd-wrong-password")
                .body(Map.of("username", "captcha_user", "password", "incorrect"))
                .post("/app/session/pwd").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldExposeSafePublicConfigurationForEachEndWithoutAuthentication() {
        for (String endpoint : new String[]{"/common/captcha/config", "/admin/captcha/config"}) {
            String body = request().get(endpoint).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.enabled", equalTo(true))
                    .body("data.siteKey", equalTo("oa-test"))
                    .body("data.serverUrl", equalTo("https://captcha.public.test"))
                    .extract().asString();
            assertThat(body).doesNotContain(SECRET, "127.0.0.1", "verifySecret", "verifyUrl", "allowedHostnames");
        }
        request().get("/common/system/info").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(CALLS.get()).isZero();
    }

    @Test
    void shouldNotAddVerificationToEmailSendingOrRefresh() {
        request().body(Map.of("email", "captcha@test.example", "businessType", "LOGIN"))
                .post("/app/session/email/code").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        request().body(Map.of("refreshToken", "not-a-refresh-token"))
                .post("/app/session/refresh").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        assertThat(CALLS.get()).isZero();
    }

    private Map<String, String> credentials() {
        return Map.of("username", "captcha_user", "password", "123456");
    }

    private static HttpServer startVerifier() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/siteverify", exchange -> {
                CALLS.incrementAndGet();
                JSONObject body = JSON.parseObject(
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                String token = body.getString("response");
                boolean valid = SECRET.equals(body.getString("secret"))
                        && !Set.of("invalid", "expired").contains(token) && SPENT.add(token);
                String action = token.startsWith("admin-") ? "admin_login"
                        : token.startsWith("email-") ? "app_login_email" : "app_login_pwd";
                byte[] bytes = JSON.toJSONBytes(Map.of("success", valid,
                        "hostname", token.equals("wrong-host") ? "other.test" : "oa.test.longlian.online",
                        "action", action));
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(token.equals("unavailable") ? 503 : 200, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            return server;
        } catch (IOException e) {
            throw new ExceptionInInitializerError(e);
        }
    }
}

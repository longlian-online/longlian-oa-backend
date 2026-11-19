package online.longlian.app.service.captcha;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.sun.net.httpserver.HttpServer;
import online.longlian.app.common.enumeration.CaptchaAction;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.CaptchaProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.CaptchaVerifyParamsBO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CaptchaServiceTest {
    private final AtomicReference<JSONObject> submitted = new AtomicReference<>();
    private HttpServer server;
    private ExecutorService executor;
    private CaptchaProperties properties;
    private CaptchaService service;
    private String response = "{\"success\":true,\"hostname\":\"oa.test\",\"action\":\"app_login_pwd\"}";
    private int status = 200;
    private long delay;

    @BeforeEach
    void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        executor = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(executor);
        server.createContext("/siteverify", exchange -> {
            submitted.set(JSON.parseObject(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
            try {
                if (delay > 0) Thread.sleep(delay);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        properties = new CaptchaProperties();
        properties.setEnabled(true);
        properties.setServerUrl(URI.create("https://captcha.test"));
        properties.setSiteKey("oa-test");
        properties.setVerifyUrl(URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/siteverify"));
        properties.setVerifySecret("private-verify-credential");
        properties.setAllowedHostnames(Set.of("oa.test"));
        properties.validate();
        service = new CaptchaService(properties);
    }

    @AfterEach
    void tearDown() {
        service.close();
        server.stop(0);
        executor.shutdownNow();
    }

    @Test
    void shouldVerifyWithPrivateCredentialAndOmitUnassertedIp() {
        assertThatCode(() -> service.verify(params())).doesNotThrowAnyException();
        assertThat(submitted.get()).containsEntry("secret", "private-verify-credential")
                .containsEntry("response", "browser-token").doesNotContainKey("remoteip");
        assertThat(service.getConfig().getServerUrl()).isEqualTo("https://captcha.test");
        assertThat(service.getConfig().getScriptUrl()).isEqualTo("https://captcha.test/fcaptcha.js");
        assertThat(service.getConfig().getSiteKey()).isEqualTo("oa-test");
    }

    @Test
    void shouldBindExplicitVisitorIpWhenEnabled() {
        properties.setBindIp(true);
        assertThatCode(() -> service.verify(params())).doesNotThrowAnyException();
        assertThat(submitted.get()).containsEntry("remoteip", "192.0.2.5");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"success\":true,\"hostname\":\"other.test\",\"action\":\"app_login_pwd\"}",
            "{\"success\":true,\"hostname\":\"oa.test\",\"action\":\"admin_login\"}",
            "{\"success\":true,\"action\":\"app_login_pwd\"}",
            "{\"success\":false,\"error-codes\":[\"timeout-or-duplicate\"]}",
            "{\"success\":false,\"error-codes\":[\"invalid-input-response\"]}"
    })
    void shouldDenyInvalidOrMismatchedVerdict(String verdict) {
        response = verdict;
        assertFailure(ResultCode.CAPTCHA_INVALID);
    }

    @ParameterizedTest
    @ValueSource(strings = {"not-json", "null", "{}", "{\"success\":\"true\"}",
            "{\"success\":false,\"error-codes\":[\"invalid-input-secret\"]}",
            "{\"success\":false,\"error-codes\":[\"internal-error\"]}"})
    void shouldDenyMalformedOrUnavailableVerifier(String verdict) {
        response = verdict;
        assertFailure(ResultCode.CAPTCHA_UNAVAILABLE);
    }

    @ParameterizedTest
    @ValueSource(ints = {302, 401, 429, 503})
    void shouldDenyHttpFailuresWithoutFollowingRedirects(int httpStatus) {
        status = httpStatus;
        assertFailure(ResultCode.CAPTCHA_UNAVAILABLE);
    }

    @Test
    void shouldBoundVerifierResponseTime() {
        properties.setReadTimeout(Duration.ofMillis(100));
        delay = 1000;
        assertFailure(ResultCode.CAPTCHA_UNAVAILABLE);
    }

    @Test
    void shouldRejectInterruptedVerificationAndPreserveInterruptStatus() {
        Thread.currentThread().interrupt();
        try {
            assertFailure(ResultCode.CAPTCHA_UNAVAILABLE);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void shouldExposeOnlyDisabledStateWhenDisabled() {
        properties.setEnabled(false);
        assertThatCode(() -> service.verify(params())).doesNotThrowAnyException();
        assertThat(service.getConfig().isEnabled()).isFalse();
        assertThat(service.getConfig().getServerUrl()).isNull();
        assertThat(service.getConfig().getScriptUrl()).isNull();
        assertThat(service.getConfig().getSiteKey()).isNull();
        assertThat(submitted.get()).isNull();
    }

    private CaptchaVerifyParamsBO params() {
        return CaptchaVerifyParamsBO.builder().token("browser-token")
                .action(CaptchaAction.APP_LOGIN_PWD).remoteIp("192.0.2.5").build();
    }

    private void assertFailure(ResultCode code) {
        assertThatThrownBy(() -> service.verify(params())).isInstanceOfSatisfying(AppException.class,
                exception -> assertThat(exception.getCode()).isEqualTo(code.getCode()));
    }
}

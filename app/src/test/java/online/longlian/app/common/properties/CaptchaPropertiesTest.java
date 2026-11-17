package online.longlian.app.common.properties;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class CaptchaPropertiesTest {
    private final ApplicationContextRunner context = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class);

    @Test
    void shouldPermitExplicitlyDisabledConfigurationWithoutSecrets() {
        context.run(application -> {
            assertThat(application).hasNotFailed();
            assertThat(application.getBean(CaptchaProperties.class).isEnabled()).isFalse();
        });
    }

    @Test
    void shouldRejectEnabledConfigurationWithoutRequiredValues() {
        context.withPropertyValues("longlian.captcha.enabled=true")
                .run(application -> assertThat(application).hasFailed());
    }

    @ParameterizedTest
    @ValueSource(strings = {"verify-secret=", "server-url=ftp://captcha.test",
            "verify-url=http://user:secret@private.test/siteverify",
            "allowed-hostnames=https://oa.test", "allowed-hostnames=oa.test/path", "read-timeout=0s"})
    void shouldRejectUnsafeOrIncompleteEnabledConfiguration(String override) {
        validContext().withPropertyValues("longlian.captcha." + override)
                .run(application -> assertThat(application).hasFailed());
    }

    @Test
    void shouldRejectEnabledConfigurationWithoutAllowedHostnames() {
        validContext().withPropertyValues("longlian.captcha.allowed-hostnames=")
                .run(application -> {
                    assertThat(application).hasFailed();
                    assertThat(application.getStartupFailure())
                            .hasRootCauseMessage("启用人机验证必须配置 allowed-hostnames");
                });
    }

    @Test
    void shouldRejectBlankHostnameInAllowlist() {
        validContext().withPropertyValues("longlian.captcha.allowed-hostnames=oa.test, ")
                .run(application -> {
                    assertThat(application).hasFailed();
                    assertThat(application.getStartupFailure())
                            .hasRootCauseMessage("allowed-hostnames 必须是前端页面主机名");
                });
    }

    @Test
    void shouldNormalizeConfiguredFrontendHostname() {
        validContext().withPropertyValues("longlian.captcha.allowed-hostnames=OA.TEST")
                .run(application -> {
                    assertThat(application).hasNotFailed();
                    assertThat(application.getBean(CaptchaProperties.class).getAllowedHostnames()).containsExactly("oa.test");
                });
    }

    private ApplicationContextRunner validContext() {
        return context.withPropertyValues("longlian.captcha.enabled=true",
                "longlian.captcha.server-url=https://captcha.test",
                "longlian.captcha.site-key=oa-test",
                "longlian.captcha.verify-url=http://private.test/siteverify",
                "longlian.captcha.verify-secret=private-credential",
                "longlian.captcha.allowed-hostnames=oa.test");
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(CaptchaProperties.class)
    static class PropertiesConfiguration { }
}

package online.longlian.app.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerDefaultsTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources().remove("systemEnvironment"))
            .withInitializer(new ConfigDataApplicationContextInitializer());

    /** Documentation must be disabled in default and production profiles. */
    @Test
    void shouldDisableDocumentationByDefault() {
        for (String profile : new String[]{"default", "prod"}) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
                assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
            });
        }
    }

    /** Development profiles explicitly enable documentation. */
    @Test
    void shouldEnableDocumentationInDevelopment() {
        for (String profile : new String[]{"dev", "local"}) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isTrue();
                assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isTrue();
            });
        }
    }

    /** An explicit deployment override takes priority over development defaults. */
    @Test
    void shouldHonorExplicitDisable() {
        runner.withPropertyValues("spring.profiles.active=dev", "SPRINGDOC_ENABLED=false").run(context -> {
            assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
        });
    }
}

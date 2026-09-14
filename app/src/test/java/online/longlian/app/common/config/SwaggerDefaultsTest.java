package online.longlian.app.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerDefaultsTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withInitializer(context -> context.getEnvironment().getPropertySources().remove("systemEnvironment"))
            .withInitializer(new ConfigDataApplicationContextInitializer());

    /** 默认和生产 profile 必须关闭接口文档。 */
    @Test
    void shouldDisableDocumentationByDefault() {
        for (String profile : new String[]{"default", "prod"}) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
                assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
            });
        }
    }

    /** 开发 profile 显式启用接口文档。 */
    @Test
    void shouldEnableDocumentationInDevelopment() {
        for (String profile : new String[]{"dev", "local"}) {
            runner.withPropertyValues("spring.profiles.active=" + profile).run(context -> {
                assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isTrue();
                assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isTrue();
            });
        }
    }

    /** 部署时的显式配置优先于开发环境默认值。 */
    @Test
    void shouldHonorExplicitDisable() {
        runner.withPropertyValues("spring.profiles.active=dev", "SPRINGDOC_ENABLED=false").run(context -> {
            assertThat(context.getEnvironment().getProperty("springdoc.api-docs.enabled", Boolean.class)).isFalse();
            assertThat(context.getEnvironment().getProperty("springdoc.swagger-ui.enabled", Boolean.class)).isFalse();
        });
    }
}

package online.longlian.app.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerDefaultsTest {
    /** 默认配置模板必须关闭接口文档。 */
    @Test
    void shouldDisableDocumentationByDefault() throws IOException {
        assertSwaggerEnabled("application.yml.example", false);
    }

    private void assertSwaggerEnabled(String resource, boolean expected) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(resource, new ClassPathResource(resource));
        assertThat(sources).isNotEmpty();
        PropertySource<?> source = sources.get(0);
        String expectedValue = "${SPRINGDOC_ENABLED:" + expected + "}";
        assertThat(source.getProperty("springdoc.api-docs.enabled")).isEqualTo(expectedValue);
        assertThat(source.getProperty("springdoc.swagger-ui.enabled")).isEqualTo(expectedValue);
    }

    /** 开发 profile 必须显式开启接口文档。 */
    @Test
    void shouldEnableDocumentationInDevelopment() throws IOException {
        for (String profile : new String[]{"application-dev.yml", "application-local.yml"}) {
            assertSwaggerEnabled(profile, true);
        }
    }
}

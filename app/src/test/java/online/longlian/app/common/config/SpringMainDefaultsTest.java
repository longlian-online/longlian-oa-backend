package online.longlian.app.common.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringMainDefaultsTest {

    /** 本地配置模板必须提供 spring.main 的有效子配置。 */
    @Test
    void shouldEnableCircularReferencesInApplicationTemplate() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml.example", new ClassPathResource("application.yml.example"));

        assertThat(sources).isNotEmpty();
        assertThat(sources.getFirst().getProperty("spring.main.allow-circular-references")).isEqualTo(true);
    }

    /** 本地模板必须提供 OpenAPI 使用的对外服务地址。 */
    @Test
    void shouldConfigureLocalServerUrlInApplicationTemplate() throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load("application.yml.example", new ClassPathResource("application.yml.example"));

        assertThat(sources).isNotEmpty();
        assertThat(sources.getFirst().getProperty("longlian.server-url")).isEqualTo("http://localhost:10003");
    }
}

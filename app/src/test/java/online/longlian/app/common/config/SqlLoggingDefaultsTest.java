package online.longlian.app.common.config;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLoggingDefaultsTest {
    /** 外部配置模板必须使用 SLF4J，避免把 SQL 直接写入标准输出。 */
    @Test
    void shouldUseSlf4jInApplicationTemplate() throws IOException {
        assertLogImplementation("application.yml.example", Slf4jImpl.class.getName());
    }

    /** 测试环境使用无日志实现，避免测试输出被 SQL 淹没。 */
    @Test
    void shouldDisableSqlLoggingInTestProfile() throws IOException {
        assertLogImplementation("application-test.yml", "org.apache.ibatis.logging.nologging.NoLoggingImpl");
    }

    private void assertLogImplementation(String resource, String expected) throws IOException {
        List<PropertySource<?>> sources = new YamlPropertySourceLoader()
                .load(resource, new ClassPathResource(resource));
        assertThat(sources).isNotEmpty();
        assertThat(sources.get(0).getProperty("mybatis-plus.configuration.log-impl"))
                .isEqualTo(expected);
    }
}

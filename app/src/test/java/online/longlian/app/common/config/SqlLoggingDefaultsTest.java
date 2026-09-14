package online.longlian.app.common.config;

import org.apache.ibatis.logging.slf4j.Slf4jImpl;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SqlLoggingDefaultsTest {
    /** SQL output must use the configurable logging backend in all application profiles. */
    @Test
    void shouldUseSlf4jInDefaultProfile() {
        assertSlf4jLogging("default");
    }

    @Test
    void shouldUseSlf4jInDevelopmentProfile() {
        assertSlf4jLogging("dev");
    }

    @Test
    void shouldUseSlf4jInProductionProfile() {
        assertSlf4jLogging("prod");
    }

    private void assertSlf4jLogging(String profile) {
        new ApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withPropertyValues("spring.profiles.active=" + profile)
                .run(context -> assertThat(context.getEnvironment()
                        .getProperty("mybatis-plus.configuration.log-impl"))
                        .isEqualTo(Slf4jImpl.class.getName()));
    }
}

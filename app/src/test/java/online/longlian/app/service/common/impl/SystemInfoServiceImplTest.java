package online.longlian.app.service.common.impl;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SystemInfoServiceImplTest {

    /** 正式版与开发版均保留构建版本原值，避免写死版本或丢失后缀。 */
    @ParameterizedTest
    @ValueSource(strings = {"2.3.4", "2.3.5-SNAPSHOT", "3.0.0-RC1"})
    void shouldReturnBackendBuildVersion(String version) {
        Properties properties = new Properties();
        properties.setProperty("version", version);
        SystemInfoServiceImpl service = new SystemInfoServiceImpl(new BuildProperties(properties));

        assertThat(service.getSystemInfo().getVersion()).isEqualTo(version);
    }
}

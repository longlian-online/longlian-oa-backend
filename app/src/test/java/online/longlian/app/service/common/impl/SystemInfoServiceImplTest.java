package online.longlian.app.service.common.impl;

import org.junit.jupiter.api.Test;
import org.springframework.boot.info.BuildProperties;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class SystemInfoServiceImplTest {

    @Test
    void shouldReturnUtcBuildTime() {
        Properties properties = new Properties();
        properties.setProperty("time", "2026-10-19T02:30:00Z");
        SystemInfoServiceImpl service = new SystemInfoServiceImpl(new BuildProperties(properties));

        assertThat(service.getSystemInfo().getBuildTime()).isEqualTo("2026-10-19T02:30:00Z");
    }

    @Test
    void shouldLeaveMissingBuildTimeUnknown() {
        SystemInfoServiceImpl service = new SystemInfoServiceImpl(new BuildProperties(new Properties()));

        assertThat(service.getSystemInfo().getBuildTime()).isNull();
    }
}

package online.longlian.app.common.config;

import io.swagger.v3.oas.models.servers.Server;
import online.longlian.app.common.properties.LonglianProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SwaggerConfigTest {

    /** OpenAPI 文档必须使用配置的对外部署地址，而不是代理到应用的内部协议。 */
    @Test
    void shouldUseConfiguredServerUrl() {
        LonglianProperties longlianProperties = new LonglianProperties();
        longlianProperties.setServerUrl("https://api.test.longlian.online");

        Server server = new SwaggerConfig(longlianProperties)
                .customOpenAPI()
                .getServers()
                .getFirst();

        assertThat(server.getUrl()).isEqualTo("https://api.test.longlian.online");
    }
}

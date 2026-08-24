package online.longlian.app.common.config;

import online.longlian.app.common.properties.CorsProperties;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsConfigTest {

    @Test
    void shouldBuildSourceWithConfiguredAllowedOriginsAndMethods() {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(List.of("https://sit.neo.oa.longlian.online"));

        CorsConfiguration config = new CorsConfig(properties).corsConfigurationSource()
                .getCorsConfiguration(new MockHttpServletRequest("GET", "/app/session/pwd"));

        assertThat(config).isNotNull();
        assertThat(config.getAllowedOrigins()).containsExactly("https://sit.neo.oa.longlian.online");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "OPTIONS");
        assertThat(config.getAllowedHeaders()).contains("*");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
        // 认证走 JWT 请求头，不依赖 Cookie，故不放行凭据
        assertThat(config.getAllowCredentials()).isFalse();
    }
}

package online.longlian.app.common.config;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.properties.CorsProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * CORS 跨域配置。
 * <p>
 * 认证走 Authorization 请求头（JWT），不依赖 Cookie，所以不放行凭据（allowCredentials=false），
 * 浏览器预检不会携带 Cookie，来源只能用显式白名单而不能用通配，安全与功能均满足。
 */
@Configuration
@RequiredArgsConstructor
public class CorsConfig {

    private static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
    );

    private final CorsProperties corsProperties;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsProperties.getAllowedOrigins());
        config.setAllowedMethods(ALLOWED_METHODS);
        // 预检不携带 Cookie，允许任意请求头，后续新增自定义请求头时无需再改这里
        config.setAllowedHeaders(List.of("*"));
        // 认证走 JWT 请求头，不依赖 Cookie，因此不放行凭据，避免 CORS 与 Cookie 的默认跨域风险
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}

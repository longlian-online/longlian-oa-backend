package online.longlian.app.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * CORS 跨域配置。
 * <p>
 * 默认不声明任何来源（不允许跨域），由各环境通过 CORS_ALLOWED_ORIGINS 显式声明前端域名，
 * 避免开成通配导致任意站点都能跨域调用。
 */
@ConfigurationProperties(prefix = "cors")
@Data
public class CorsProperties {
    private List<String> allowedOrigins = List.of();
}

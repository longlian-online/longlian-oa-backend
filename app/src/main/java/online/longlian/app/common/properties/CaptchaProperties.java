package online.longlian.app.common.properties;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 通用 FCaptcha 人机验证配置。启用时缺少必要配置直接拒绝启动，避免半开状态。
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "longlian.captcha")
public class CaptchaProperties {
    private boolean enabled;
    private URI serverUrl;
    private String siteKey;
    private URI verifyUrl;
    private String verifySecret;
    private Set<String> allowedHostnames = Set.of();
    private Duration connectTimeout = Duration.ofSeconds(2);
    private Duration readTimeout = Duration.ofSeconds(5);
    private boolean bindIp;

    @PostConstruct
    public void validate() {
        if (!enabled) return;
        requireHttpUrl(serverUrl, "server-url");
        requireHttpUrl(verifyUrl, "verify-url");
        if (!StringUtils.hasText(siteKey) || !StringUtils.hasText(verifySecret)) {
            throw new IllegalStateException("启用人机验证必须配置 site-key 和 verify-secret");
        }
        if (allowedHostnames == null || allowedHostnames.isEmpty()) {
            throw new IllegalStateException("启用人机验证必须配置 allowed-hostnames");
        }
        allowedHostnames = allowedHostnames.stream().map(host -> {
            if (!StringUtils.hasText(host)) {
                throw new IllegalStateException("allowed-hostnames 必须是前端页面主机名");
            }
            String normalized = host.trim().toLowerCase(Locale.ROOT);
            URI parsed = URI.create("https://" + normalized);
            if (!normalized.equals(parsed.getHost()) || parsed.getPort() != -1
                    || parsed.getRawUserInfo() != null || !parsed.getRawPath().isEmpty()
                    || parsed.getRawQuery() != null || parsed.getRawFragment() != null) {
                throw new IllegalStateException("allowed-hostnames 必须是前端页面主机名");
            }
            return normalized;
        }).collect(Collectors.toUnmodifiableSet());
        if (connectTimeout == null || connectTimeout.isNegative() || connectTimeout.isZero()
                || readTimeout == null || readTimeout.isNegative() || readTimeout.isZero()) {
            throw new IllegalStateException("人机验证超时必须大于零");
        }
    }

    private void requireHttpUrl(URI uri, String name) {
        if (uri == null || uri.getHost() == null
                || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                || uri.getRawUserInfo() != null || uri.getRawQuery() != null || uri.getRawFragment() != null) {
            throw new IllegalStateException("启用人机验证必须配置有效的 " + name);
        }
    }
}

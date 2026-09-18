package online.longlian.app.service.resource;

import online.longlian.app.common.properties.StorageProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

/**
 * Produces private storage URLs using the configured CDN path-token protocol.
 */
@Component
public final class CdnUrlSigner {

    private final String urlPrefix;
    private final String authKey;
    private final Clock clock;

    @Autowired
    public CdnUrlSigner(StorageProperties properties, Clock clock) {
        this(properties.getCdn(), clock);
    }

    public CdnUrlSigner(StorageProperties.CdnConfig config, Clock clock) {
        this(config == null ? null : config.getUrlPrefix(), config == null ? null : config.getAuthKey(), clock);
    }

    public CdnUrlSigner(String urlPrefix, String authKey, Clock clock) {
        this.urlPrefix = urlPrefix;
        this.authKey = authKey;
        this.clock = clock;
    }

    public String sign(String storageKey) {
        if (!StringUtils.hasText(storageKey) || storageKey.startsWith("/")) {
            throw new IllegalArgumentException("非法文件 key");
        }
        return signPath("/" + UriUtils.encodePath(storageKey, StandardCharsets.UTF_8), "");
    }

    /**
     * 签名 CDN 回源路径。查询参数不参与路径令牌计算，调用方必须自行保护可篡改参数。
     */
    public String signPath(String path, String query) {
        if (!StringUtils.hasText(urlPrefix) || !StringUtils.hasText(authKey)) {
            throw new IllegalStateException("CDN 读取域名和鉴权密钥必须配置");
        }
        if (!StringUtils.hasText(path) || !path.startsWith("/") || path.contains("?") || path.contains("#")) {
            throw new IllegalArgumentException("非法读取路径");
        }

        URI baseUri = parseBaseUri();
        String requestPath = buildPath(baseUri, path);
        long timestamp = clock.instant().getEpochSecond();
        String token = md5(authKey + requestPath + timestamp);
        String queryPrefix = StringUtils.hasText(query) ? query + "&" : "";
        return baseUri.getScheme() + "://" + baseUri.getRawAuthority() + requestPath
                + "?" + queryPrefix + "token=" + token + "&t=" + timestamp;
    }

    private URI parseBaseUri() {
        try {
            URI baseUri = URI.create(urlPrefix);
            if (!baseUri.isAbsolute() || !StringUtils.hasText(baseUri.getRawAuthority())
                    || baseUri.getRawQuery() != null || baseUri.getRawFragment() != null
                    || baseUri.getRawUserInfo() != null) {
                throw invalidBaseUrl();
            }
            return baseUri;
        } catch (IllegalArgumentException e) {
            throw invalidBaseUrl();
        }
    }

    private String buildPath(URI baseUri, String path) {
        String basePath = baseUri.getRawPath();
        int end = basePath.length();
        while (end > 0 && basePath.charAt(end - 1) == '/') {
            end--;
        }
        return basePath.substring(0, end) + path;
    }

    private String md5(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        // CDN 路径令牌协议要求 MD5；JDK 保证该算法存在。
        } catch (NoSuchAlgorithmException e) { // skipcq: TCV-001
            throw new IllegalStateException("无法生成 CDN 鉴权签名", e); // skipcq: TCV-001
        }
    }

    private IllegalStateException invalidBaseUrl() {
        return new IllegalStateException("CDN 读取域名必须是无查询参数的绝对 URL");
    }
}

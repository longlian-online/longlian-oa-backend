package online.longlian.app.service.resource;

import org.springframework.util.StringUtils;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.HexFormat;

/**
 * Produces EdgeOne URL-authentication method D links for private COS objects.
 */
public final class EdgeOneUrlSigner {

    private final String urlPrefix;
    private final String authKey;
    private final Clock clock;

    public EdgeOneUrlSigner(String urlPrefix, String authKey, Clock clock) {
        this.urlPrefix = urlPrefix;
        this.authKey = authKey;
        this.clock = clock;
    }

    public String sign(String storageKey) {
        if (!StringUtils.hasText(urlPrefix) || !StringUtils.hasText(authKey)) {
            throw new IllegalStateException("COS 的 EdgeOne 读取域名和鉴权密钥必须配置");
        }
        if (!StringUtils.hasText(storageKey) || storageKey.startsWith("/")) {
            throw new IllegalArgumentException("非法文件 key");
        }

        URI baseUri = parseBaseUri();
        String path = buildPath(baseUri, storageKey);
        long timestamp = clock.instant().getEpochSecond();
        String token = md5(authKey + path + timestamp);
        return baseUri.getScheme() + "://" + baseUri.getRawAuthority() + path
                + "?token=" + token + "&t=" + timestamp;
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

    private String buildPath(URI baseUri, String storageKey) {
        String basePath = baseUri.getRawPath();
        int end = basePath.length();
        while (end > 0 && basePath.charAt(end - 1) == '/') {
            end--;
        }
        return basePath.substring(0, end) + "/" + UriUtils.encodePath(storageKey, StandardCharsets.UTF_8);
    }

    private String md5(String source) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("MD5")
                    .digest(source.getBytes(StandardCharsets.UTF_8)));
        // EdgeOne 方法 D 固定要求 MD5；JDK 保证该算法存在。
        } catch (NoSuchAlgorithmException e) { // skipcq: TCV-001
            throw new IllegalStateException("无法生成 EdgeOne 鉴权签名", e); // skipcq: TCV-001
        }
    }

    private IllegalStateException invalidBaseUrl() {
        return new IllegalStateException("COS 的 EdgeOne 读取域名必须是无查询参数的绝对 URL");
    }
}

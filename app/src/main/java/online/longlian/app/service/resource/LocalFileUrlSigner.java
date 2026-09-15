package online.longlian.app.service.resource;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.util.HexFormat;

@Component
public class LocalFileUrlSigner {
    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String READ_SIGNATURE_DOMAIN = "longlian:local-file-read:v1";
    private static final String UPLOAD_SIGNATURE_DOMAIN = "longlian:local-file-upload:v1";
    private static final String PAYLOAD_SEPARATOR = "\n";

    private final SecretKeySpec key;
    private final Clock clock;
    private final long ttlSeconds;

    public LocalFileUrlSigner(
            @Value("${storage.local.signing-secret:${jwt.secret}}") String secret,
            @Value("${storage.local.read-url-ttl-seconds:300}") long ttlSeconds,
            Clock clock) {
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32 || ttlSeconds <= 0) {
            throw new IllegalArgumentException("本地文件签名密钥至少需要 32 字节，链接有效期必须为正数");
        }
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM);
        this.clock = clock;
        this.ttlSeconds = ttlSeconds;
    }

    public LocalFileReadParamsBO sign(String storageKey) {
        return sign(storageKey, READ_SIGNATURE_DOMAIN);
    }

    public void verify(LocalFileReadParamsBO params) {
        verify(params, READ_SIGNATURE_DOMAIN);
    }

    public LocalFileReadParamsBO signUpload(String storageKey) {
        return sign(storageKey, UPLOAD_SIGNATURE_DOMAIN);
    }

    public void verifyUpload(LocalFileReadParamsBO params) {
        verify(params, UPLOAD_SIGNATURE_DOMAIN);
    }

    private LocalFileReadParamsBO sign(String storageKey, String domain) {
        long expires = Math.addExact(clock.instant().getEpochSecond(), ttlSeconds);
        return new LocalFileReadParamsBO(storageKey, expires, signature(domain, storageKey, expires));
    }

    private void verify(LocalFileReadParamsBO params, String domain) {
        if (params.expires() <= clock.instant().getEpochSecond()
                || !MessageDigest.isEqual(signature(domain, params.key(), params.expires()).getBytes(StandardCharsets.US_ASCII),
                params.signature().getBytes(StandardCharsets.US_ASCII))) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "文件链接无效或已过期");
        }
    }

    private String signature(String domain, String storageKey, long expires) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(key);
            // 使用独立的签名域，避免文件签名被当作其他凭证复用。
            String payload = domain + PAYLOAD_SEPARATOR + expires + PAYLOAD_SEPARATOR + storageKey;
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        // JDK 保证 HmacSHA256 算法存在，该异常分支无法通过测试触发。
        } catch (GeneralSecurityException e) { // skipcq: TCV-001
            throw new IllegalStateException("无法生成文件签名", e); // skipcq: TCV-001
        }
    }
}

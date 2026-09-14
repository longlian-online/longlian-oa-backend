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
        this.key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.clock = clock;
        this.ttlSeconds = ttlSeconds;
    }

    public LocalFileReadParamsBO sign(String storageKey) {
        long expires = Math.addExact(clock.instant().getEpochSecond(), ttlSeconds);
        return new LocalFileReadParamsBO(storageKey, expires, signature(storageKey, expires));
    }

    public void verify(LocalFileReadParamsBO params) {
        if (params.expires() <= clock.instant().getEpochSecond()
                || !MessageDigest.isEqual(signature(params.key(), params.expires()).getBytes(StandardCharsets.US_ASCII),
                params.signature().getBytes(StandardCharsets.US_ASCII))) {
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "文件链接无效或已过期");
        }
    }

    private String signature(String storageKey, long expires) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(key);
            // 使用独立的签名域，避免文件签名被当作其他凭证复用。
            String payload = "longlian:local-file-read:v1\n" + expires + "\n" + storageKey;
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        // JDK 保证 HmacSHA256 算法存在，该异常分支无法通过测试触发。
        } catch (GeneralSecurityException e) { // skipcq: TCV-001
            throw new IllegalStateException("无法生成文件签名", e); // skipcq: TCV-001
        }
    }
}

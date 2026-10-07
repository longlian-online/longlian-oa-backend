package online.longlian.app.service.resource;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;

class LocalFileUrlSignerTest {
    private static final String SECRET = "test-local-signing-secret-32-bytes";
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private final LocalFileUrlSigner signer = signer(300, clock);

    /** 有效签名只授权对应的 key，直到链接过期为止。 */
    @Test
    void shouldAcceptValidSignature() {
        LocalFileReadParamsBO signed = signer.sign("avatar/中文 图片.png");
        assertThatCode(() -> signer.verify(signed)).doesNotThrowAnyException();
        assertThat(signed.expires()).isEqualTo(clock.instant().getEpochSecond() + 300);
    }

    /** key、过期时间和签名都受到完整性保护。 */
    @Test
    void shouldRejectTampering() {
        LocalFileReadParamsBO signed = signer.sign("avatar/1.png");
        assertThatThrownBy(() -> signer.verify(new LocalFileReadParamsBO("avatar/2.png", signed.expires(), signed.signature())))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> signer.verify(new LocalFileReadParamsBO(signed.key(), signed.expires() + 1, signed.signature())))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> signer.verify(new LocalFileReadParamsBO(signed.key(), signed.expires(), "0".repeat(64))))
                .isInstanceOf(AppException.class);
    }

    /** 到达过期时间后链接立即超出有效窗口。 */
    @Test
    void shouldRejectAtExpiryBoundary() {
        LocalFileReadParamsBO signed = signer.sign("avatar/1.png");
        LocalFileUrlSigner later = signer(300, Clock.offset(clock, java.time.Duration.ofSeconds(300)));
        assertThatThrownBy(() -> later.verify(signed)).isInstanceOf(AppException.class);
    }

    /** 使用另一部署密钥生成的签名不能授权读取。 */
    @Test
    void shouldRejectSignatureFromDifferentSecret() {
        LocalFileUrlSigner other = signer("another-local-signing-secret-32-bytes", 300, clock);
        assertThatThrownBy(() -> signer.verify(other.sign("avatar/1.png"))).isInstanceOf(AppException.class);
    }

    /** 读取签名不能授权上传，上传签名也不能授权读取。 */
    @Test
    void shouldRejectCrossDomainSignatures() {
        LocalFileReadParamsBO read = signer.sign("avatar/1.png");
        LocalFileReadParamsBO upload = signer.signUpload("avatar/1.png");
        assertThatThrownBy(() -> signer.verifyUpload(read)).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> signer.verify(upload)).isInstanceOf(AppException.class);
        assertThatCode(() -> signer.verifyUpload(upload)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> signer("too-short", 300, clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> signer(SECRET, 0, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /** 本地签名链接使用统一的存储预签名有效期。 */
    @Test
    void shouldUseConfiguredPresignedUrlTtl() {
        LocalFileReadParamsBO signed = signer(120, clock).sign("avatar/1.png");

        assertThat(signed.expires()).isEqualTo(clock.instant().getEpochSecond() + 120);
    }

    /** 指定过期时间签发用于 CDN 分桶，过期值不能重新授权本地文件。 */
    @Test
    void shouldRejectExpiredExplicitReadExpiry() {
        assertThatThrownBy(() -> signer.sign("avatar/1.png", clock.instant().getEpochSecond()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("文件链接过期时间必须在未来");
    }

    private LocalFileUrlSigner signer(long ttlSeconds, Clock signerClock) {
        return signer(SECRET, ttlSeconds, signerClock);
    }

    private LocalFileUrlSigner signer(String secret, long ttlSeconds, Clock signerClock) {
        StorageProperties properties = new StorageProperties();
        properties.setPresignedUrlTtlSeconds(ttlSeconds);
        return new LocalFileUrlSigner(secret, properties, signerClock);
    }
}

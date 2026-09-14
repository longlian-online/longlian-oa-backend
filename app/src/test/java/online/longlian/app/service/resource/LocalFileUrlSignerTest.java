package online.longlian.app.service.resource;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocalFileUrlSignerTest {
    private static final String SECRET = "test-local-signing-secret-32-bytes";
    private final Clock clock = Clock.fixed(Instant.parse("2026-09-14T00:00:00Z"), ZoneOffset.UTC);
    private final LocalFileUrlSigner signer = new LocalFileUrlSigner(SECRET, 300, clock);

    /** 有效签名只授权对应的 key，直到链接过期为止。 */
    @Test
    void shouldAcceptValidSignature() {
        var signed = signer.sign("avatar/中文 图片.png");
        assertThatCode(() -> signer.verify(signed)).doesNotThrowAnyException();
        assertThat(signed.expires()).isEqualTo(clock.instant().getEpochSecond() + 300);
    }

    /** key、过期时间和签名都受到完整性保护。 */
    @Test
    void shouldRejectTampering() {
        var signed = signer.sign("avatar/1.png");
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
        var signed = signer.sign("avatar/1.png");
        var later = new LocalFileUrlSigner(SECRET, 300, Clock.offset(clock, java.time.Duration.ofSeconds(300)));
        assertThatThrownBy(() -> later.verify(signed)).isInstanceOf(AppException.class);
    }

    /** 无效签名会在查询资源元数据或文件系统前被拒绝。 */
    @Test
    void shouldRejectBeforeResourceLookup() {
        ResourceService resources = mock(ResourceService.class);
        var reader = new LocalFileReadService(signer, resources);
        assertThatThrownBy(() -> reader.read(new LocalFileReadParamsBO("avatar/1.png", 1, "0".repeat(64))))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(resources);
    }

    /** 使用另一部署密钥生成的签名不能授权读取。 */
    @Test
    void shouldRejectSignatureFromDifferentSecret() {
        var other = new LocalFileUrlSigner("another-local-signing-secret-32-bytes", 300, clock);
        assertThatThrownBy(() -> signer.verify(other.sign("avatar/1.png"))).isInstanceOf(AppException.class);
    }

    @Test
    void shouldRejectInvalidConfiguration() {
        assertThatThrownBy(() -> new LocalFileUrlSigner("too-short", 300, clock))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LocalFileUrlSigner(SECRET, 0, clock))
                .isInstanceOf(IllegalArgumentException.class);
    }
}

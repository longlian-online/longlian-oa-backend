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

    /** Valid signatures authorize the exact key until expiry. */
    @Test
    void shouldAcceptValidSignature() {
        var signed = signer.sign("avatar/中文 图片.png");
        assertThatCode(() -> signer.verify(signed)).doesNotThrowAnyException();
        assertThat(signed.expires()).isEqualTo(clock.instant().getEpochSecond() + 300);
    }

    /** Key, expiry and signature are all integrity protected. */
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

    /** The expiry instant is already outside the validity window. */
    @Test
    void shouldRejectAtExpiryBoundary() {
        var signed = signer.sign("avatar/1.png");
        var later = new LocalFileUrlSigner(SECRET, 300, Clock.offset(clock, java.time.Duration.ofSeconds(300)));
        assertThatThrownBy(() -> later.verify(signed)).isInstanceOf(AppException.class);
    }

    /** Invalid signatures are rejected before querying resource metadata or the filesystem. */
    @Test
    void shouldRejectBeforeResourceLookup() {
        ResourceService resources = mock(ResourceService.class);
        var reader = new LocalFileReadService(signer, resources);
        assertThatThrownBy(() -> reader.read(new LocalFileReadParamsBO("avatar/1.png", 1, "0".repeat(64))))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(resources);
    }

    /** Signing with another deployment key cannot authorize a read. */
    @Test
    void shouldRejectSignatureFromDifferentSecret() {
        var other = new LocalFileUrlSigner("another-local-signing-secret-32-bytes", 300, clock);
        assertThatThrownBy(() -> signer.verify(other.sign("avatar/1.png"))).isInstanceOf(AppException.class);
    }
}

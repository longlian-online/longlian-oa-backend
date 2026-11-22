package online.longlian.app.common.util;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.bo.open.OpenCursorBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OpenCursorCodecTest {

    @Test
    void shouldRoundTripUpdatedAtAndIdCursor() {
        OpenCursorBO cursor = new OpenCursorBO(11L, 1700000000000L);

        OpenCursorBO decoded = OpenCursorCodec.decode(OpenCursorCodec.encode(cursor));

        assertThat(decoded.afterId()).isEqualTo(11L);
        assertThat(decoded.afterUpdatedAtMillis()).isEqualTo(1700000000000L);
    }

    @Test
    void shouldRoundTripIdOnlyCursor() {
        OpenCursorBO cursor = new OpenCursorBO(20L, null);

        OpenCursorBO decoded = OpenCursorCodec.decode(OpenCursorCodec.encode(cursor));

        assertThat(decoded.afterId()).isEqualTo(20L);
        assertThat(decoded.afterUpdatedAtMillis()).isNull();
    }

    @Test
    void shouldRejectCursorThatIsNotBase64() {
        assertThatThrownBy(() -> OpenCursorCodec.decode("不是base64!!"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("游标格式错误");
    }

    @Test
    void shouldRejectCursorWithUnknownVersionOrFormat() {
        String wrongVersion = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v9:1:1".getBytes());
        String wrongParts = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1:1".getBytes());
        String nonNumeric = java.util.Base64.getUrlEncoder().withoutPadding()
                .encodeToString("v1:abc:1".getBytes());

        assertThatThrownBy(() -> OpenCursorCodec.decode(wrongVersion))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> OpenCursorCodec.decode(wrongParts))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> OpenCursorCodec.decode(nonNumeric))
                .isInstanceOf(AppException.class);
    }

    @Test
    void shouldProduceKnownSha256HexVector() {
        assertThat(Sha256Util.sha256Hex("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
    }
}

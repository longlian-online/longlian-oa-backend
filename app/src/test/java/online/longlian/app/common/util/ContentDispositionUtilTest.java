package online.longlian.app.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContentDispositionUtilTest {

    /** 中文文件名走 filename* 百分号编码，兜底名把非 ASCII 替换为下划线保留扩展名。 */
    @Test
    void shouldBuildAttachmentHeaderWithUtf8EncodedFileName() {
        assertThat(ContentDispositionUtil.attachment("我的文件.docx"))
                .isEqualTo("attachment; filename=\"____.docx\""
                        + "; filename*=UTF-8''%E6%88%91%E7%9A%84%E6%96%87%E4%BB%B6.docx");
    }

    @Test
    void shouldPercentEncodeSpaceAndQuote() {
        assertThat(ContentDispositionUtil.attachment("a b\"c.png"))
                .isEqualTo("attachment; filename=\"a b\\\"c.png\""
                        + "; filename*=UTF-8''a%20b%22c.png");
    }

    @Test
    void shouldEscapeBackslashInQuotedFallback() {
        assertThat(ContentDispositionUtil.attachment("a\\b.png"))
                .isEqualTo("attachment; filename=\"a\\\\b.png\""
                        + "; filename*=UTF-8''a%5Cb.png");
    }

    /** 控制字符（含 CRLF）必须剔除，杜绝响应头注入。 */
    @Test
    void shouldStripControlCharsToPreventHeaderInjection() {
        assertThat(ContentDispositionUtil.attachment("a\r\nX-Injected: 1\r\nb.png"))
                .isEqualTo("attachment; filename=\"aX-Injected: 1b.png\""
                        + "; filename*=UTF-8''aX-Injected%3A%201b.png");
    }

    @Test
    void shouldFallBackToFixedNameWhenNothingUsable() {
        assertThat(ContentDispositionUtil.attachment(""))
                .isEqualTo("attachment; filename=\"file\"; filename*=UTF-8''file");
        assertThat(ContentDispositionUtil.attachment("？？"))
                .isEqualTo("attachment; filename=\"__\"; filename*=UTF-8''%EF%BC%9F%EF%BC%9F");
    }
}

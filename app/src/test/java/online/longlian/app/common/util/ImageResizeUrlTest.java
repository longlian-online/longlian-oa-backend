package online.longlian.app.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResizeUrlTest {

    @Test
    void shouldAppendResizeParamWithTheExistingQueryDelimiter() {
        assertThat(ImageResizeUrl.append("https://cdn/cover.png", "eo-img.resize=w/400/h/600"))
                .isEqualTo("https://cdn/cover.png?eo-img.resize=w/400/h/600");
        assertThat(ImageResizeUrl.append("https://cdn/cover.png?sign=ok", "eo-img.resize=w/64/h/64"))
                .isEqualTo("https://cdn/cover.png?sign=ok&eo-img.resize=w/64/h/64");
    }
}

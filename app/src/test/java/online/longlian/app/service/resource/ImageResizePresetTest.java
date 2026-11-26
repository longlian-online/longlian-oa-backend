package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ImageResizePresetTest {

    @Test
    void apply_appendsPresetWithQuestionMarkOrAmpersand() {
        assertThat(ImageResizePreset.AVATAR.apply("https://cdn.example/avatar.png"))
                .isEqualTo("https://cdn.example/avatar.png?eo-img.resize=w/64/h/64");
        assertThat(ImageResizePreset.AVATAR.apply("https://cdn.example/avatar.png?token=abc&t=1"))
                .isEqualTo("https://cdn.example/avatar.png?token=abc&t=1&eo-img.resize=w/64/h/64");
        assertThat(ImageResizePreset.THUMBNAIL.apply("https://cdn.example/thumb.png"))
                .isEqualTo("https://cdn.example/thumb.png?eo-img.resize=w/144/h/144");
        assertThat(ImageResizePreset.COVER.apply("https://cdn.example/cover.png?t=1"))
                .isEqualTo("https://cdn.example/cover.png?t=1&eo-img.resize=w/400/h/600");
    }

    @Test
    void apply_preservesNullBlankAndAlreadyResizedUrls() {
        assertThat(ImageResizePreset.AVATAR.apply((String) null)).isNull();
        assertThat(ImageResizePreset.AVATAR.apply("")).isEmpty();
        assertThat(ImageResizePreset.AVATAR.apply("   ")).isEqualTo("   ");
        String resized = "https://cdn.example/avatar.png?token=abc&eo-img.resize=w/64/h/64";
        assertThat(ImageResizePreset.AVATAR.apply(resized)).isEqualTo(resized);
    }

    @Test
    void applyMap_resizesPresentUrlsAndSkipsNullEntries() {
        Map<Long, ResourceReadUrlGetResultBO> input = new HashMap<>();
        input.put(1L, new ResourceReadUrlGetResultBO("https://cdn.example/a.png", 1L, "a"));
        input.put(2L, new ResourceReadUrlGetResultBO("https://cdn.example/b.png?t=1", 1L, "b"));
        input.put(3L, null);
        input.put(4L, new ResourceReadUrlGetResultBO(null, 1L, "c"));

        Map<Long, String> result = ImageResizePreset.AVATAR.apply(input);

        assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
                1L, "https://cdn.example/a.png?eo-img.resize=w/64/h/64",
                2L, "https://cdn.example/b.png?t=1&eo-img.resize=w/64/h/64"
        ));
        assertThat(ImageResizePreset.AVATAR.apply((Map<Long, ResourceReadUrlGetResultBO>) null)).isEmpty();
        assertThat(ImageResizePreset.AVATAR.apply(Map.of())).isEmpty();
    }
}

package online.longlian.app.common.validation;

import online.longlian.app.common.constants.PatternConstants;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class FileTypePatternTest {

    private static final Pattern FILE_EXT = Pattern.compile(PatternConstants.FILE_EXT_PATTERN);
    private static final Pattern FILE_MIME = Pattern.compile(PatternConstants.FILE_MIME_PATTERN);

    @Test
    void acceptsMp4AndWebmForAttachments() {
        assertThat(FILE_EXT.matcher("mp4").matches()).isTrue();
        assertThat(FILE_EXT.matcher("WEBM").matches()).isTrue();
        assertThat(FILE_MIME.matcher("video/mp4").matches()).isTrue();
        assertThat(FILE_MIME.matcher("video/webm").matches()).isTrue();
        assertThat(FILE_EXT.matcher("mp3").matches()).isTrue();
        assertThat(FILE_EXT.matcher("m4a").matches()).isTrue();
        assertThat(FILE_MIME.matcher("audio/mpeg").matches()).isTrue();
        assertThat(FILE_MIME.matcher("audio/mp4").matches()).isTrue();
    }

    @Test
    void rejectsVideoContainersTheBrowserCannotPreview() {
        assertThat(FILE_EXT.matcher("mov").matches()).isFalse();
        assertThat(FILE_EXT.matcher("mkv").matches()).isFalse();
        assertThat(FILE_EXT.matcher("mid").matches()).isFalse();
        assertThat(FILE_MIME.matcher("video/quicktime").matches()).isFalse();
        assertThat(FILE_MIME.matcher("video/x-matroska").matches()).isFalse();
        assertThat(FILE_MIME.matcher("audio/midi").matches()).isFalse();
    }
}

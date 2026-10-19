package online.longlian.app.common.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WeakPasswordDictionaryTest {

    private final WeakPasswordDictionary dictionary = new WeakPasswordDictionary();

    /** 常见密码及大小写变体均应命中字典。 */
    @ParameterizedTest
    @ValueSource(strings = {"123456", "654321", "111111", "qwerty", "password", "Password", "PASSWORD", "PaSsWoRd"})
    void shouldRecognizeCommonPasswordsIgnoringCase(String password) {
        assertThat(dictionary.contains(password)).isTrue();
    }

    /** 完整密码匹配允许包含常见单词的不同密码，且不擅自裁剪空格。 */
    @ParameterizedTest
    @ValueSource(strings = {"River!82Maple", "a-password-b", " password ", "x7!Q2z"})
    void shouldAllowPasswordsOutsideDictionary(String password) {
        assertThat(dictionary.contains(password)).isFalse();
    }

    /** 字典缺失必须立即失败，不能降级为空黑名单。 */
    @Test
    void shouldFailWhenDictionaryIsMissing() {
        assertThatThrownBy(() -> new WeakPasswordDictionary(new ClassPathResource("missing-password-dictionary.txt")))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(IOException.class);
    }

    /** 空文件和仅含空白行的文件都无法提供有效保护。 */
    @ParameterizedTest
    @ValueSource(strings = {"", "\n \n\t\n"})
    void shouldFailWhenDictionaryIsEmpty(String content) {
        assertThatThrownBy(() -> new WeakPasswordDictionary(new ByteArrayResource(content.getBytes(StandardCharsets.UTF_8))))
                .isInstanceOf(IllegalStateException.class);
    }
}

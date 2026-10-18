package online.longlian.app.pojo.dto.app;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class RegisterByInviteDTOTest {

    @ParameterizedTest
    @ValueSource(strings = {"user1", "用户名称", "ab-c", "ab.c", "ab c", "ab\tc", "abcd\n", "café", "ＡＢＣＤ", "ab😀"})
    void shouldRejectUsernameWithDisallowedCharacters(String username) {
        RegisterByInviteDTO dto = new RegisterByInviteDTO();
        dto.setUsername(username);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(dto, "username"))
                    .extracting(ConstraintViolation::getInvalidValue)
                    .containsExactly(username);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abcd", "ABCD", "____", "_Ab_", "Abcdefghijklmnopqrs_"})
    void shouldAcceptUsernameContainingOnlyEnglishLettersAndUnderscores(String username) {
        RegisterByInviteDTO dto = new RegisterByInviteDTO();
        dto.setUsername(username);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(dto, "username")).isEmpty();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "abcdefghijklmnopqrstu"})
    void shouldRejectUsernameOutsideLengthBounds(String username) {
        RegisterByInviteDTO dto = new RegisterByInviteDTO();
        dto.setUsername(username);

        try (var factory = Validation.buildDefaultValidatorFactory()) {
            assertThat(factory.getValidator().validateProperty(dto, "username"))
                    .extracting(ConstraintViolation::getInvalidValue)
                    .containsExactly(username);
        }
    }
}

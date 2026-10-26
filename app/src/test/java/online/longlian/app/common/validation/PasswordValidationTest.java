package online.longlian.app.common.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import online.longlian.app.common.constants.PatternConstants;
import online.longlian.app.pojo.dto.app.ChangePasswordDTO;
import online.longlian.app.pojo.dto.app.LoginByPwdDTO;
import online.longlian.app.pojo.dto.app.RegisterByInviteDTO;
import online.longlian.app.pojo.dto.app.ResetPasswordDTO;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidationTest {

    private static LocalValidatorFactoryBean validator;

    @BeforeAll
    static void setUpValidator() {
        validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();
    }

    @AfterAll
    static void closeValidator() {
        validator.close();
    }

    static Stream<Arguments> passwordFields() {
        return Stream.of(
                Arguments.of(RegisterByInviteDTO.class, "password"),
                Arguments.of(ResetPasswordDTO.class, "password"),
                Arguments.of(ChangePasswordDTO.class, "newPassword"));
    }

    /** 注册、重置和修改的新密码必须同时包含大写、小写和数字。 */
    @ParameterizedTest
    @MethodSource("passwordFields")
    void shouldRejectNewPasswordsMissingCharacterClasses(Class<?> dto, String property) {
        for (String password : List.of(
                "123456", "password", "PASSWORD", "PaSsWoRd", "a-password-b", " password ",
                "Abcdefgh", "ABCDEF1", "abcdef1")) {
            assertThat(validator.validateValue(dto, property, password))
                    .singleElement().satisfies(violation -> {
                        assertThat(violation.getConstraintDescriptor().getAnnotation()).isInstanceOf(Pattern.class);
                        assertThat(violation.getMessage()).isEqualTo(PatternConstants.PASSWORD_COMPLEXITY_MESSAGE);
                    });
        }
    }

    /** 同时包含三类字符时可通过，包括原先会被字典拒绝的 Password1；6 位和 20 位边界继续有效。 */
    @ParameterizedTest
    @MethodSource("passwordFields")
    void shouldAcceptNewPasswordsWithUpperLowerAndDigit(Class<?> dto, String property) {
        for (String password : List.of("GoodPass1", "Password1", "x7!Q2z", "Ab1cde", "x7!Q2z9@Lm4#Rt8$Vb6%")) {
            assertThat(validator.validateValue(dto, property, password)).isEmpty();
        }
    }

    /** 非空约束和长度约束继续各自负责基础参数错误。 */
    @ParameterizedTest
    @MethodSource("passwordFields")
    void shouldPreserveRequiredAndLengthConstraints(Class<?> dto, String property) {
        assertThat(validator.validateValue(dto, property, null))
                .singleElement().satisfies(violation -> assertThat(violation.getConstraintDescriptor().getAnnotation())
                        .isInstanceOf(NotBlank.class));
        for (String password : List.of("", "   ", "Ab1c", "GoodPass1GoodPass1XYZ")) {
            assertThat(validator.validateValue(dto, property, password))
                    .anySatisfy(violation -> assertThat(violation.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(Size.class));
        }
    }

    /** 登录和原密码不套用复杂度，已有弱口令仍可验证身份；过短仍然被长度约束拒绝。 */
    @Test
    void shouldAcceptExistingWeakPasswordForAuthentication() {
        for (String password : List.of("123456", "password", "a-password-b")) {
            assertThat(validator.validateValue(LoginByPwdDTO.class, "password", password)).isEmpty();
            assertThat(validator.validateValue(ChangePasswordDTO.class, "oldPassword", password)).isEmpty();
        }
        assertThat(validator.validateValue(LoginByPwdDTO.class, "password", "Ab1"))
                .singleElement().satisfies(violation -> assertThat(violation.getConstraintDescriptor().getAnnotation())
                        .isInstanceOf(Size.class));
    }
}

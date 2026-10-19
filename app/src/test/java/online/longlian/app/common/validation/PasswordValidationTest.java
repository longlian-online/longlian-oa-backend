package online.longlian.app.common.validation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import online.longlian.app.common.annotation.NotWeakPassword;
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
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.validation.beanvalidation.SpringConstraintValidatorFactory;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordValidationTest {

    private static LocalValidatorFactoryBean validator;

    @BeforeAll
    static void setUpValidator() {
        var beans = new DefaultListableBeanFactory();
        beans.registerSingleton("weakPasswordDictionary", new WeakPasswordDictionary());
        validator = new LocalValidatorFactoryBean();
        validator.setConstraintValidatorFactory(new SpringConstraintValidatorFactory(beans));
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

    /** 三种设置密码的 DTO 均使用黑名单校验。 */
    @ParameterizedTest
    @MethodSource("passwordFields")
    void shouldRejectWeakNewPasswords(Class<?> dto, String property) {
        for (String password : List.of("123456", "654321", "password", "PASSWORD", "PaSsWoRd")) {
            assertThat(validator.validateValue(dto, property, password))
                    .singleElement().satisfies(violation -> assertThat(violation.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(NotWeakPassword.class));
        }
    }

    /** 黑名单之外的密码可通过，原有 6 和 20 位边界继续有效。 */
    @ParameterizedTest
    @MethodSource("passwordFields")
    void shouldAcceptNonCommonNewPasswordsWithinLengthBounds(Class<?> dto, String property) {
        for (String password : List.of("River!82Maple", "a-password-b", " password ", "x7!Q2z", "x7!Q2z9@Lm4#Rt8$Vb6%")) {
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
        for (String password : List.of("", "   ", "x7!Q2", "x7!Q2z9@Lm4#Rt8$Vb6%X")) {
            assertThat(validator.validateValue(dto, property, password))
                    .anySatisfy(violation -> assertThat(violation.getConstraintDescriptor().getAnnotation())
                            .isInstanceOf(Size.class));
        }
    }

    /** 已有弱密码仍可用于登录和修改密码时验证旧凭证。 */
    @Test
    void shouldAcceptExistingWeakPasswordForAuthentication() {
        assertThat(validator.validateValue(LoginByPwdDTO.class, "password", "123456")).isEmpty();
        assertThat(validator.validateValue(ChangePasswordDTO.class, "oldPassword", "123456")).isEmpty();
    }
}

package online.longlian.app.common.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import online.longlian.app.common.validation.NotWeakPasswordValidator;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Target(FIELD)
@Retention(RUNTIME)
@Constraint(validatedBy = NotWeakPasswordValidator.class)
public @interface NotWeakPassword {
    String message() default "密码过于简单，请尝试更换更复杂的密码";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

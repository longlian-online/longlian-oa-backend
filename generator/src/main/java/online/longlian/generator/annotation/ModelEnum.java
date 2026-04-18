package online.longlian.generator.annotation;

import jakarta.validation.constraints.NotNull;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ModelEnums.class)
public @interface ModelEnum {
    @NotNull String model();
    @NotNull String field();
}

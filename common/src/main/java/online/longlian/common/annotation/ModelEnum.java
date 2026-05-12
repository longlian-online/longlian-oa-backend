package online.longlian.common.annotation;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(ModelEnums.class)
public @interface ModelEnum {
    String model();
    String field();
}

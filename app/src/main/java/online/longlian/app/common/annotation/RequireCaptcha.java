package online.longlian.app.common.annotation;

import online.longlian.app.common.enumeration.CaptchaAction;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明该接口必须先通过对应场景的人机验证。场景由后端决定，前端传入值只用于取票。
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireCaptcha {
    CaptchaAction value();
}

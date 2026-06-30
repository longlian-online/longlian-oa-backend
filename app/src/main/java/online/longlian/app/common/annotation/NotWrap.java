package online.longlian.app.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记 Controller 方法或类跳过 Result 自动包装
 * <p>
 * 用于文件下载、回调等需要返回原始响应的场景，避免 ResponseBodyAdvice 将其包装为 {@link online.longlian.app.common.result.Result}
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotWrap {
}

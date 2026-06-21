package online.longlian.app.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 自定义 Controller 方法成功时的提示消息
 * <p>
 * 未标注时默认使用 {@link online.longlian.app.common.result.ResultCode#SUCCESS} 的 msg
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseMessage {
    /** 成功提示消息 */
    String value();
}

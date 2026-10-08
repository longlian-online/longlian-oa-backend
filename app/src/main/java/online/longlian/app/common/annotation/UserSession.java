package online.longlian.app.common.annotation;

import online.longlian.app.common.enumeration.OrganizationDeclaration;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 声明用户令牌接口是否读取组织。{@code value} 没有默认值。
 * 需要会话参数时标在参数上；不需要参数时标在方法上。同一方法只能有一处。
 */
@Target({ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface UserSession {

    OrganizationDeclaration value();
}

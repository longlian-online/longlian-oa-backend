package online.longlian.app.common.security;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/** 在安全过滤器边界中传递可公开的业务错误。 */
@Getter
public class RequestAuthenticationException extends AuthenticationException {
    private final int code;

    public RequestAuthenticationException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

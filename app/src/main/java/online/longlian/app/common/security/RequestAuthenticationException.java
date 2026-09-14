package online.longlian.app.common.security;

import lombok.Getter;
import org.springframework.security.core.AuthenticationException;

/** Carries a public business error through the security filter boundary. */
@Getter
public class RequestAuthenticationException extends AuthenticationException {
    private final int code;

    public RequestAuthenticationException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}

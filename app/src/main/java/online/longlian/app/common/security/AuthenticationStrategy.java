package online.longlian.app.common.security;

import org.springframework.security.core.Authentication;

public interface AuthenticationStrategy {

    String supportedType();

    Authentication authenticate(long subjectId);
}

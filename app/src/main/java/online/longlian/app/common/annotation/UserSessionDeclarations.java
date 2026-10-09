package online.longlian.app.common.annotation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public final class UserSessionDeclarations {

    private UserSessionDeclarations() {
    }

    public static List<UserSession> of(Method method) {
        List<UserSession> found = new ArrayList<>();
        UserSession onMethod = method.getAnnotation(UserSession.class);
        if (onMethod != null) {
            found.add(onMethod);
        }
        for (Parameter parameter : method.getParameters()) {
            UserSession onParameter = parameter.getAnnotation(UserSession.class);
            if (onParameter != null) {
                found.add(onParameter);
            }
        }
        return found;
    }
}

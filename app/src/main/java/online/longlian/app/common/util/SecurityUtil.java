package online.longlian.app.common.util;

import lombok.NoArgsConstructor;
import online.longlian.app.common.security.UserDetailImpl;
import org.springframework.security.core.context.SecurityContextHolder;

@NoArgsConstructor
public class SecurityUtil {

    public static UserDetailImpl getCurrentUser() {
        return (UserDetailImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    public static Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}

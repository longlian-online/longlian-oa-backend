package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 提供请求层读取认证上下文的能力，避免业务服务直接依赖线程安全上下文。
 */
@Component
public class CurrentUserContext {

    public UserDetailImpl requireUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailImpl userDetail)) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return userDetail;
    }

    public Long requireUserId() {
        return requireUser().getId();
    }

    public Long getAdminId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AdminUserDetails adminUserDetails) {
            return adminUserDetails.getId();
        }
        return null;
    }
}

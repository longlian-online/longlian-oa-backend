package online.longlian.app.common.security;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * 提供请求层读取当前用户的能力，避免参数解析器依赖完整会话服务。
 */
@Component
public class CurrentUserContext {

    public Long requireUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserDetailImpl userDetail)) {
            throw new AppException(ResultCode.UNAUTHORIZED);
        }
        return userDetail.getId();
    }
}

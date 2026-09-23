package online.longlian.app.common.resolver;

import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.CurrentUserContext;
import online.longlian.app.common.security.UserDetailImpl;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserSessionArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserContext currentUserContext;

    public UserSessionArgumentResolver(CurrentUserContext currentUserContext) {
        this.currentUserContext = currentUserContext;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UserSession.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UserDetailImpl userDetail = currentUserContext.requireUser();
        UserSession annotation = parameter.getParameterAnnotation(UserSession.class);
        if (annotation != null && annotation.required() && userDetail.getCurrentOrgId() == null) {
            throw new AppException(ResultCode.OPERATION_FAIL, "当前组织不存在，请重新选择组织");
        }
        return new SessionContext(userDetail.getId(), userDetail.getCurrentOrgId(), userDetail.getSessionId());
    }
}

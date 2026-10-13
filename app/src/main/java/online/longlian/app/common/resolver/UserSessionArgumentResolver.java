package online.longlian.app.common.resolver;

import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.interceptor.OrganizationScopeInterceptor;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.CurrentUserContext;
import online.longlian.app.common.security.OrganizationScope;
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
        Long userId = currentUserContext.requireUserId();
        UserSession annotation = parameter.getParameterAnnotation(UserSession.class);
        if (annotation == null || annotation.value() == OrganizationDeclaration.NONE) {
            return new SessionContext(userId, null);
        }
        Object value = webRequest.getAttribute(
                OrganizationScopeInterceptor.SCOPE_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);
        if (!(value instanceof OrganizationScope scope)) {
            throw new AppException(ResultCode.OPERATION_FAIL, "组织不能为空");
        }
        return new SessionContext(userId, scope.orgId());
    }
}

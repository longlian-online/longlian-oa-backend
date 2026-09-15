package online.longlian.app.common.resolver;

import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.security.CurrentUserContext;
import online.longlian.app.service.common.CurrentOrganizationService;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class UserSessionArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserContext currentUserContext;
    private final CurrentOrganizationService currentOrganizationService;

    public UserSessionArgumentResolver(CurrentUserContext currentUserContext,
                                        CurrentOrganizationService currentOrganizationService) {
        this.currentUserContext = currentUserContext;
        this.currentOrganizationService = currentOrganizationService;
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
        Long orgId;
        if (annotation != null && annotation.required()) {
            orgId = currentOrganizationService.requireCurrentOrgId(userId);
        } else {
            orgId = currentOrganizationService.resolveCurrentOrgId(userId);
        }
        return new SessionContext(userId, orgId);
    }
}

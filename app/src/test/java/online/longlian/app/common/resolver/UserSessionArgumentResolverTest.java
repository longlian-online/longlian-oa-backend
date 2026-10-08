package online.longlian.app.common.resolver;

import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.security.CurrentUserContext;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserSessionArgumentResolverTest {

    @Test
    void shouldRejectRequiredOrganizationWhenScopeIsMissing() throws Exception {
        CurrentUserContext currentUserContext = mock(CurrentUserContext.class);
        when(currentUserContext.requireUserId()).thenReturn(1L);
        UserSessionArgumentResolver resolver = new UserSessionArgumentResolver(currentUserContext);

        assertThatThrownBy(() -> resolver.resolveArgument(
                parameter("required"), null,
                new ServletWebRequest(new MockHttpServletRequest()), null))
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getCode())
                                .isEqualTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    private MethodParameter parameter(String methodName) throws NoSuchMethodException {
        Method method = Sample.class.getMethod(methodName, SessionContext.class);
        return new MethodParameter(method, 0);
    }

    public static class Sample {

        public void required(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext sessionContext) {
        }
    }
}

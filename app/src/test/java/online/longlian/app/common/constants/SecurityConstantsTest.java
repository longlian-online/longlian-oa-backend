package online.longlian.app.common.constants;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConstantsTest {

    private static MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod(method);
        request.setRequestURI(path);
        request.setServletPath(path);
        return request;
    }

    private static boolean matches(List<RequestMatcher> matchers, MockHttpServletRequest request) {
        return matchers.stream().anyMatch(matcher -> matcher.matches(request));
    }

    @Test
    void shouldExcludeSwaggerPathsUnlessExplicitlyIncluded() {
        MockHttpServletRequest swaggerUi = request("GET", "/swagger-ui/index.html");
        MockHttpServletRequest apiDocs = request("GET", "/v3/api-docs");
        MockHttpServletRequest login = request("POST", "/app/session/pwd");

        List<RequestMatcher> withoutSwagger = SecurityConstants.getPermitAllMatchers(false);
        assertThat(matches(withoutSwagger, swaggerUi)).isFalse();
        assertThat(matches(withoutSwagger, apiDocs)).isFalse();
        assertThat(matches(withoutSwagger, login)).isTrue();

        List<RequestMatcher> withSwagger = SecurityConstants.getPermitAllMatchers(true);
        assertThat(matches(withSwagger, swaggerUi)).isTrue();
        assertThat(matches(withSwagger, apiDocs)).isTrue();
        assertThat(matches(withSwagger, login)).isTrue();
    }

    @Test
    void defaultPermitAllMatchersShouldNotIncludeSwagger() {
        MockHttpServletRequest swaggerUi = request("GET", "/swagger-ui.html");
        assertThat(matches(SecurityConstants.getPermitAllMatchers(), swaggerUi)).isFalse();
        assertThat(matches(SecurityConstants.getSwaggerPermitAllMatchers(), swaggerUi)).isTrue();
    }
}

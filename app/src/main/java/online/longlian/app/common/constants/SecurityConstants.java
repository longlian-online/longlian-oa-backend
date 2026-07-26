package online.longlian.app.common.constants;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import java.util.List;

public class SecurityConstants {
    private static final List<RequestMatcher> PERMIT_ALL_MATCHERS = List.of(
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/app/session/pwd"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/app/session/email"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/admin/session"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/app/session/email/code"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/app/user/register/create-organization"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/app/user/register/join-organization"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/app/user/register/join-organization/invite-info"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui.html"),
            PathPatternRequestMatcher.pathPattern("/swagger-ui/**"),
            PathPatternRequestMatcher.pathPattern("/v3/api-docs/**"),
            PathPatternRequestMatcher.pathPattern("/swagger-resources/**"),
            PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/common/file/local"),
            PathPatternRequestMatcher.pathPattern("/error")
    );

    /**
     * 免鉴权路径白名单。返回不可变列表，防止调用方篡改鉴权规则。
     */
    public static List<RequestMatcher> getPermitAllMatchers() {
        return PERMIT_ALL_MATCHERS;
    }

    private SecurityConstants() {
    }
}

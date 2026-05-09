package online.longlian.app.common.constants;

import org.springframework.http.HttpMethod;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class SecurityConstants {
    public static final RequestMatcher[] PERMIT_ALL_MATCHERS = {
            new AntPathRequestMatcher("/app/session/pwd", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/app/session/email", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/admin/session", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/app/session/email/code", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/app/user/register/create-organization", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/app/user/register/join-organization", HttpMethod.POST.name()),
            new AntPathRequestMatcher("/app/user/register/join-organization/invite-info", HttpMethod.GET.name()),
            new AntPathRequestMatcher("/swagger-ui.html"),
            new AntPathRequestMatcher("/swagger-ui/**"),
            new AntPathRequestMatcher("/v3/api-docs/**"),
            new AntPathRequestMatcher("/swagger-resources/**")
    };

    private SecurityConstants() {
    }
}

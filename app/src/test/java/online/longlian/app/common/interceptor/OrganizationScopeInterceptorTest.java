package online.longlian.app.common.interceptor;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.annotation.UserSession;
import online.longlian.app.common.enumeration.OrganizationDeclaration;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.security.AdminUserDetails;
import online.longlian.app.common.security.OrganizationScope;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.entity.OrganizationMember;
import online.longlian.app.service.common.OrganizationMembershipService;
import online.longlian.common.enumeration.Status;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationScopeInterceptorTest {

    @Mock
    private OrganizationMembershipService organizationMembershipService;

    private OrganizationScopeInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new OrganizationScopeInterceptor(organizationMembershipService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReplaceAuthoritiesWhenMethodRequiresOrganization() throws Exception {
        UserDetailImpl user = user(1L);
        UsernamePasswordAuthenticationToken incoming = new UsernamePasswordAuthenticationToken(
                user, "credential", List.of(new SimpleGrantedAuthority("ROLE_ORG_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(incoming);
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "12");
        when(organizationMembershipService.requireEnabledMember(1L, 12L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(12L).userId(1L).orgRole("ORG_USER").status(Status.ENABLED).build());

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isTrue();

        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ORG_USER");
        assertThat(request.getAttribute(OrganizationScopeInterceptor.SCOPE_ATTRIBUTE))
                .isEqualTo(new OrganizationScope(12L, 9L, "ORG_USER"));
    }

    @Test
    void shouldRejectMissingHeaderWhenMethodRequiresOrganization() throws Exception {
        authenticateUser();

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        verifyNoInteractions(organizationMembershipService);
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectNonUserPrincipalAsUnauthorized() throws Exception {
        AdminUserDetails admin = AdminUserDetails.from(1L, "admin", "ADMIN");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(admin, null, admin.getAuthorities()));
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "12");

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        verifyNoInteractions(organizationMembershipService);
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldIgnoreHeaderWhenMethodDeclaresNone() throws Exception {
        UsernamePasswordAuthenticationToken incoming = authentication();
        SecurityContextHolder.getContext().setAuthentication(incoming);
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "abc");

        assertThat(interceptor.preHandle(request, response, handler("none", SessionContext.class))).isTrue();

        verifyNoInteractions(organizationMembershipService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(incoming);
    }

    @Test
    void shouldIgnoreHeaderWhenHandlerHasNoDeclaration() throws Exception {
        UsernamePasswordAuthenticationToken incoming = new UsernamePasswordAuthenticationToken(
                AdminUserDetails.from(1L, "admin", "ADMIN"), null,
                AdminUserDetails.from(1L, "admin", "ADMIN").getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(incoming);
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "abc");

        assertThat(interceptor.preHandle(request, response, handler("undecorated"))).isTrue();

        verifyNoInteractions(organizationMembershipService);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(incoming);
    }

    @Test
    void shouldRejectBlankAbcAndZeroHeaders() throws Exception {
        for (String header : List.of("   ", "abc", "0")) {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, header);
            SecurityContextHolder.getContext().setAuthentication(authentication());

            assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                    .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        }
        verifyNoInteractions(organizationMembershipService);
    }

    @Test
    void shouldClearSecurityContextWhenMembershipRejected() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L))
                .thenThrow(new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员"));

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectInvalidOrgRole() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(8L).userId(1L).orgRole("MEMBER").status(Status.ENABLED).build());

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldFailWhenMembershipQueryHitsTheDatabase() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L))
                .thenThrow(new DataRetrievalFailureException("db"));

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectOrgIdThatOverflowsLong() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, "9".repeat(30));

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        verifyNoInteractions(organizationMembershipService);
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
    }

    @Test
    void shouldLeaveCommittedResponseUntouched() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeInterceptor.ORG_ID_HEADER, " ");
        response.getWriter().write("already");
        response.flushBuffer();

        assertThat(interceptor.preHandle(request, response, handler("required", SessionContext.class))).isFalse();

        assertThat(response.getContentAsString()).isEqualTo("already");
    }

    @Test
    void shouldRejectDuplicateDeclarations() throws Exception {
        authenticateUser();

        assertThat(interceptor.preHandle(request, response, handler("duplicate", SessionContext.class))).isFalse();

        verifyNoInteractions(organizationMembershipService);
        assertThat(JSON.parseObject(response.getContentAsString()).getString("msg")).contains("组织声明重复");
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                user(1L), null, List.of(new SimpleGrantedAuthority("ROLE_ORG_ADMIN")));
    }

    private UserDetailImpl user(long id) {
        return UserDetailImpl.builder().id(id).username("user").status(Status.ENABLED).build();
    }

    private HandlerMethod handler(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return new HandlerMethod(new Sample(), Sample.class.getMethod(name, parameterTypes));
    }

    public static class Sample {

        @UserSession(OrganizationDeclaration.REQUIRED)
        public void required(SessionContext session) {
        }

        @UserSession(OrganizationDeclaration.NONE)
        public void none(SessionContext session) {
        }

        public void undecorated() {
        }

        @UserSession(OrganizationDeclaration.NONE)
        public void duplicate(@UserSession(OrganizationDeclaration.REQUIRED) SessionContext session) {
        }
    }
}

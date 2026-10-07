package online.longlian.app.common.filter;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.FilterChain;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
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

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationScopeFilterTest {

    @Mock
    private OrganizationMembershipService organizationMembershipService;
    @Mock
    private FilterChain filterChain;

    private OrganizationScopeFilter filter;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new OrganizationScopeFilter(organizationMembershipService);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReplaceAuthoritiesWithHeaderMembership() throws Exception {
        UserDetailImpl user = user(1L);
        UsernamePasswordAuthenticationToken incoming = new UsernamePasswordAuthenticationToken(
                user, "credential", List.of(new SimpleGrantedAuthority("ROLE_ORG_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(incoming);
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "12");
        when(organizationMembershipService.requireEnabledMember(1L, 12L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(12L).userId(1L).orgRole("ORG_USER").status(Status.ENABLED).build());

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_ORG_USER");
        assertThat(request.getAttribute(OrganizationScopeFilter.SCOPE_ATTRIBUTE))
                .isEqualTo(new OrganizationScope(12L, 9L, "ORG_USER"));
    }

    @Test
    void shouldIgnoreMissingHeader() throws Exception {
        UserDetailImpl user = user(1L);
        UsernamePasswordAuthenticationToken incoming = new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_ORG_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(incoming);

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(organizationMembershipService);
        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isSameAs(incoming);
    }

    @Test
    void shouldRejectBlankAbcAndZeroHeadersWithoutContinuing() throws Exception {
        authenticateUser();
        for (String header : List.of("   ", "abc", "0")) {
            request = new MockHttpServletRequest();
            response = new MockHttpServletResponse();
            request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, header);
            SecurityContextHolder.getContext().setAuthentication(authentication());

            filter.doFilter(request, response, filterChain);

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                    .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        }
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        verifyNoInteractions(organizationMembershipService);
    }

    @Test
    void shouldClearSecurityContextWhenMembershipRejected() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L))
                .thenThrow(new AppException(ResultCode.OPERATION_FAIL, "您不是该组织成员"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldIgnoreBadHeaderForAdminPrincipal() throws Exception {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                AdminUserDetails.from(1L, "admin", "ADMIN"), null,
                AdminUserDetails.from(1L, "admin", "ADMIN").getAuthorities()));
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "abc");

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(organizationMembershipService);
        verify(filterChain).doFilter(request, response);
    }

    private void authenticateUser() {
        SecurityContextHolder.getContext().setAuthentication(authentication());
    }

    private UsernamePasswordAuthenticationToken authentication() {
        return new UsernamePasswordAuthenticationToken(
                user(1L), null, List.of(new SimpleGrantedAuthority("ROLE_ORG_ADMIN")));
    }

    @Test
    void shouldRejectInvalidOrgRole() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L)).thenReturn(
                OrganizationMember.builder().id(9L).orgId(8L).userId(1L).orgRole("MEMBER").status(Status.ENABLED).build());

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldFailWhenMembershipQueryHitsTheDatabase() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "8");
        when(organizationMembershipService.requireEnabledMember(1L, 8L))
                .thenThrow(new DataRetrievalFailureException("db"));

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.FAIL.getCode());
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldRejectOrgIdThatOverflowsLong() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, "9".repeat(30));

        filter.doFilter(request, response, filterChain);

        verifyNoInteractions(organizationMembershipService);
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
        assertThat(JSON.parseObject(response.getContentAsString()).getIntValue("code"))
                .isEqualTo(ResultCode.OPERATION_FAIL.getCode());
    }

    @Test
    void shouldLeaveCommittedResponseUntouched() throws Exception {
        authenticateUser();
        request.addHeader(OrganizationScopeFilter.ORG_ID_HEADER, " ");
        response.getWriter().write("already");
        response.flushBuffer();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getContentAsString()).isEqualTo("already");
        verify(filterChain, never()).doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    }

    private UserDetailImpl user(long id) {
        return UserDetailImpl.builder().id(id).username("user").status(Status.ENABLED).build();
    }
}

package online.longlian.app.mcp;

import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.server.McpSyncServerExchange;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.resolver.SessionContext;
import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.service.common.OrganizationMembershipService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class McpUserSessionTest {

    // 标准用户身份与已校验的请求范围可以跨 SDK 执行线程传递，不再依赖专用 Token 类型。
    @Test
    void shouldResolveBoundSessionWithStandardUserAuthentication() {
        OrganizationMembershipService membership = mock(OrganizationMembershipService.class);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        var authentication = new UsernamePasswordAuthenticationToken(UserDetailImpl.builder().id(7L).build(), null, List.of());
        SessionContext bound = new SessionContext(7L, 12L);
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                McpUserSession.AUTHENTICATION, authentication, SessionContext.class.getName(), bound)));
        SecurityContextHolder.clearContext();

        assertThat(new McpUserSession(membership).require(exchange)).isSameAs(bound);
        verifyNoInteractions(membership);
    }

    // 即使存在范围数据，也必须有经过认证的用户身份。
    @Test
    void shouldRejectBoundSessionWithoutAuthenticatedUser() {
        OrganizationMembershipService membership = mock(OrganizationMembershipService.class);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                SessionContext.class.getName(), new SessionContext(7L, 12L))));

        assertThatThrownBy(() -> new McpUserSession(membership).require(exchange)).isInstanceOf(AppException.class);
        verifyNoInteractions(membership);
    }

    // SDK 工作线程不需要继承 ThreadLocal；只使用当前 HTTP 请求捕获的身份和组织。
    @Test
    void shouldResolveIdentityFromTransportAndValidateMembership() {
        OrganizationMembershipService membership = mock(OrganizationMembershipService.class);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        var authentication = new UsernamePasswordAuthenticationToken(UserDetailImpl.builder().id(7L).build(), null, List.of());
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(
                McpUserSession.AUTHENTICATION, authentication, McpUserSession.ORGANIZATION, "12")));
        SecurityContextHolder.clearContext();

        var session = new McpUserSession(membership).require(exchange);

        assertThat(session.userId()).isEqualTo(7L);
        assertThat(session.orgId()).isEqualTo(12L);
        verify(membership).requireEnabledMember(7L, 12L);
    }

    // 缺少组织声明时不隐式回退用户的默认组织。
    @Test
    void shouldRejectMissingOrganization() {
        OrganizationMembershipService membership = mock(OrganizationMembershipService.class);
        McpSyncServerExchange exchange = mock(McpSyncServerExchange.class);
        var authentication = new UsernamePasswordAuthenticationToken(UserDetailImpl.builder().id(7L).build(), null, List.of());
        when(exchange.transportContext()).thenReturn(McpTransportContext.create(Map.of(McpUserSession.AUTHENTICATION, authentication)));

        assertThatThrownBy(() -> new McpUserSession(membership).require(exchange)).isInstanceOf(AppException.class);
        verifyNoInteractions(membership);
    }
}

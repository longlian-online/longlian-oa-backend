package online.longlian.app.common.handler;

import com.alibaba.fastjson2.JSON;
import jakarta.servlet.ServletException;
import online.longlian.app.common.result.Result;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class AuthenticationResponseHandlerTest {

    // MCP functional route 不参与 REST handler 查找，认证失败返回 HTTP 401。
    @Test
    void shouldReturnHttpUnauthorizedForMcpWithoutHandlerLookup() throws Exception {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthenticationEntryPointImpl(mapping).commence(request, response,
                new BadCredentialsException("bad credentials"));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEmpty();
        verifyNoInteractions(mapping);
    }

    // MCP 权限失败不能包装成 HTTP 200 的 REST Result。
    @Test
    void shouldReturnHttpForbiddenForMcp() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AccessDeniedHandlerImpl().handle(request, response, new AccessDeniedException("forbidden"));

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getContentAsString()).isEmpty();
    }

    @Test
    void shouldReturnNotFoundWhenRequestHasNoHandler() throws Exception {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/missing");
        when(mapping.getHandler(request)).thenReturn(null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthenticationEntryPointImpl(mapping).commence(request, response,
                new BadCredentialsException("bad credentials"));

        Result<?> result = JSON.parseObject(response.getContentAsString(), Result.class);
        assertThat(result.getCode()).isEqualTo(ResultCode.NOT_FOUND.getCode());
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void shouldReturnUnauthorizedAsFastjsonJson() throws Exception {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        when(mapping.getHandler(request)).thenReturn(mock(HandlerExecutionChain.class));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthenticationEntryPointImpl(mapping).commence(request, response,
                new BadCredentialsException("bad credentials"));

        Result<?> result = JSON.parseObject(response.getContentAsString(), Result.class);
        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED.getCode());
        assertThat(result.getMsg()).isEqualTo(ResultCode.UNAUTHORIZED.getMsg());
    }

    @Test
    void shouldReturnFailureWhenHandlerLookupFails() throws Exception {
        RequestMappingHandlerMapping mapping = mock(RequestMappingHandlerMapping.class);
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        when(mapping.getHandler(request)).thenThrow(new IllegalStateException("mapping unavailable"));
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AuthenticationEntryPointImpl(mapping).commence(request, response,
                new BadCredentialsException("bad credentials"));

        Result<?> result = JSON.parseObject(response.getContentAsString(), Result.class);
        assertThat(result.getCode()).isEqualTo(ResultCode.FAIL.getCode());
    }

    @Test
    void shouldReturnAccessDeniedAsFastjsonJson() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AccessDeniedHandlerImpl().handle(request, response,
                new AccessDeniedException("forbidden"));

        Result<?> result = JSON.parseObject(response.getContentAsString(), Result.class);
        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        assertThat(response.getStatus()).isEqualTo(200);
    }
}

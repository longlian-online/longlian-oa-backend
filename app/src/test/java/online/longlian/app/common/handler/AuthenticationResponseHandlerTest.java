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

class AuthenticationResponseHandlerTest {

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
    void shouldReturnForbiddenAsFastjsonJson() throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/private");
        MockHttpServletResponse response = new MockHttpServletResponse();

        new AccessDeniedHandlerImpl().handle(request, response,
                new AccessDeniedException("forbidden"));

        Result<?> result = JSON.parseObject(response.getContentAsString(), Result.class);
        assertThat(result.getCode()).isEqualTo(ResultCode.UNAUTHORIZED_OPERATION.getCode());
        assertThat(response.getStatus()).isEqualTo(403);
    }
}

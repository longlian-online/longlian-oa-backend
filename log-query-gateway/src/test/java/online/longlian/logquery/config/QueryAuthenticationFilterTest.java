package online.longlian.logquery.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;

import jakarta.servlet.FilterChain;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class QueryAuthenticationFilterTest {

    @Test
    void rejectsMissingToken() throws Exception {
        LogQueryGatewayProperties properties = new LogQueryGatewayProperties();
        properties.getAuth().setToken("expected-token");
        QueryAuthenticationFilter filter = new QueryAuthenticationFilter(properties);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(new MockHttpServletRequest(), response, mock(FilterChain.class));

        assertThat(response.getStatus()).isEqualTo(401);
    }

    @Test
    void acceptsConfiguredBearerToken() throws Exception {
        LogQueryGatewayProperties properties = new LogQueryGatewayProperties();
        properties.getAuth().setToken("expected-token");
        QueryAuthenticationFilter filter = new QueryAuthenticationFilter(properties);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer expected-token");
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        verify(chain).doFilter(eq(request), any());
    }
}

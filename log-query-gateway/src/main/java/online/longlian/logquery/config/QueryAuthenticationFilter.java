package online.longlian.logquery.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class QueryAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEALTH_PATH = "/actuator/health";

    private final LogQueryGatewayProperties properties;

    public QueryAuthenticationFilter(LogQueryGatewayProperties properties) {
        this.properties = properties;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HEALTH_PATH.equals(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!properties.getAuth().isRequired()) {
            filterChain.doFilter(request, response);
            return;
        }

        String configuredToken = properties.getAuth().getToken();
        String requestToken = bearerToken(request.getHeader(HttpHeaders.AUTHORIZATION));
        if (configuredToken.isBlank() || requestToken == null || !constantTimeEquals(configuredToken, requestToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"code\":401,\"message\":\"unauthorized\"}");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private static String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return null;
        }
        return authorization.substring("Bearer ".length()).trim();
    }

    private static boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}

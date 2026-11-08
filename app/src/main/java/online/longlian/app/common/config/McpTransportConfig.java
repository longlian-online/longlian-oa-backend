package online.longlian.app.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.jackson2.JacksonMcpJsonMapper;
import io.modelcontextprotocol.server.transport.WebMvcStreamableServerTransportProvider;
import online.longlian.app.common.interceptor.OrganizationScopeInterceptor;
import online.longlian.app.mcp.McpUserSession;
import org.springframework.ai.mcp.server.common.autoconfigure.properties.McpServerStreamableHttpProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(name = "spring.ai.mcp.server.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(McpServerStreamableHttpProperties.class)
public class McpTransportConfig {

    @Bean
    @ConditionalOnProperty(name = "spring.ai.mcp.server.protocol", havingValue = "STREAMABLE")
    public WebMvcStreamableServerTransportProvider webMvcStreamableServerTransportProvider(
            @Qualifier("mcpServerObjectMapper") ObjectMapper objectMapper,
            McpServerStreamableHttpProperties properties) {
        return WebMvcStreamableServerTransportProvider.builder()
                .jsonMapper(new JacksonMcpJsonMapper(objectMapper))
                .mcpEndpoint(properties.getMcpEndpoint())
                .keepAliveInterval(properties.getKeepAliveInterval())
                .disallowDelete(properties.isDisallowDelete())
                .contextExtractor(request -> {
                    // 工具由 SDK 调度执行；在 HTTP 请求线程捕获本次身份，不依赖工具线程的 ThreadLocal。
                    Map<String, Object> context = new HashMap<>();
                    context.put(McpUserSession.AUTHENTICATION, SecurityContextHolder.getContext().getAuthentication());
                    context.put(McpUserSession.ORGANIZATION,
                            request.headers().firstHeader(OrganizationScopeInterceptor.ORG_ID_HEADER));
                    return McpTransportContext.create(context);
                })
                .build();
    }
}

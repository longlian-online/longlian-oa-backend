package online.longlian.app.common.metrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;

/**
 * HTTP 状态码恒为 200，服务质量只看响应体 {@code Result.code}。
 * 原始 URI 带资源 ID，不能当标签；没有匹配模板时记 {@code none}。
 */
@Component
public class ApiResultMetrics {

    public static final String INSTRUMENT_NAME = "app.result";
    public static final AttributeKey<String> RESULT_CODE = AttributeKey.stringKey("result.code");
    public static final AttributeKey<String> HTTP_ROUTE = AttributeKey.stringKey("http.route");
    public static final String UNMATCHED_ROUTE = "none";

    private final LongCounter counter;

    public ApiResultMetrics(OpenTelemetry openTelemetry) {
        this.counter = openTelemetry.getMeter("online.longlian.app")
                .counterBuilder(INSTRUMENT_NAME)
                .setDescription("按 Result.code 统计的 HTTP API 响应")
                .build();
    }

    public void record(HttpServletRequest request, int code) {
        add(code, routeOf(request));
    }

    public void record(ServerHttpRequest request, int code) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            record(servletRequest.getServletRequest(), code);
            return;
        }
        add(code, UNMATCHED_ROUTE);
    }

    private void add(int code, String route) {
        counter.add(1, Attributes.of(RESULT_CODE, Integer.toString(code), HTTP_ROUTE, route));
    }

    private static String routeOf(HttpServletRequest request) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        if (pattern == null) {
            return UNMATCHED_ROUTE;
        }
        String route = pattern.toString();
        if (route == null || route.isBlank()) {
            return UNMATCHED_ROUTE;
        }
        return route;
    }
}

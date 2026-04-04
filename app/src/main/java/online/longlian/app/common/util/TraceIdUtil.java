package online.longlian.app.common.util;

import io.opentelemetry.api.trace.Span;

public class TraceIdUtil {

    /**
     * 获取当前 traceId
     */
    public static String getTraceId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getTraceId();
        }
        return "unknown-trace-id";
    }

    /**
     * 获取当前 spanId
     */
    public static String getSpanId() {
        Span currentSpan = Span.current();
        if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
            return currentSpan.getSpanContext().getSpanId();
        }
        return "unknown-span-id";
    }
}
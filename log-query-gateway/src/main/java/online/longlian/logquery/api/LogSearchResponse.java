package online.longlian.logquery.api;

import java.util.List;
import java.util.Map;

public record LogSearchResponse(
        String requestId,
        long tookMs,
        int total,
        List<Map<String, Object>> logs
) {
}

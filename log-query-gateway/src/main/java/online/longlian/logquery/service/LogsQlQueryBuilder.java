package online.longlian.logquery.service;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import online.longlian.logquery.api.LogSearchRequest;

import org.springframework.stereotype.Component;

@Component
public class LogsQlQueryBuilder {

    private static final Pattern SAFE_FIELD_VALUE = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._-]{0,99}");

    public String build(LogSearchRequest request) {
        String service = exactStreamValue(request.service(), "service");
        String environment = exactStreamValue(request.environment(), "environment");
        StringBuilder query = new StringBuilder()
                .append("{service.name=\"").append(service)
                .append("\",deployment.environment=\"").append(environment).append("\"}");

        if (request.keyword() != null && !request.keyword().isBlank()) {
            query.append(" \"").append(escapePhrase(request.keyword().trim())).append("\"");
        }
        if (request.traceId() != null && !request.traceId().isBlank()) {
            query.append(" trace_id:=").append(request.traceId());
        }
        if (request.levels() != null && !request.levels().isEmpty()) {
            String levels = request.levels().stream()
                    .map(level -> "severity_text:" + level)
                    .collect(Collectors.joining(" OR ", "(", ")"));
            query.append(" ").append(levels);
        }
        return query.toString();
    }

    private static String exactStreamValue(String value, String field) {
        if (value == null || !SAFE_FIELD_VALUE.matcher(value).matches()) {
            throw new LogQueryException(400, field + " has invalid format");
        }
        return value;
    }

    private static String escapePhrase(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}

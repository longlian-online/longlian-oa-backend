package online.longlian.logquery.service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

@Component
public class LogSanitizer {

    private static final Set<String> SENSITIVE_KEY_PARTS = Set.of(
            "password", "passwd", "token", "secret", "authorization", "cookie", "private_key", "apikey", "api_key");
    private static final Pattern SENSITIVE_MESSAGE = Pattern.compile(
            "(?i)(password|passwd|token|secret|authorization|cookie|api[_-]?key)(\\s*[:=]\\s*)[^\\s,;]+");

    public Map<String, Object> sanitize(Map<String, Object> source) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((key, value) -> {
            if (isSensitiveKey(key)) {
                sanitized.put(key, "[REDACTED]");
            } else if (value instanceof String text && "_msg".equals(key)) {
                sanitized.put(key, SENSITIVE_MESSAGE.matcher(text).replaceAll("$1$2[REDACTED]"));
            } else {
                sanitized.put(key, value);
            }
        });
        return sanitized;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "_");
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }
}

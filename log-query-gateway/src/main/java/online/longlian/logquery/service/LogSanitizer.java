package online.longlian.logquery.service;

import java.util.LinkedHashMap;
import java.util.List;
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
        return sanitizeMap(source);
    }

    private static Map<String, Object> sanitizeMap(Map<?, ?> source) {
        Map<String, Object> sanitized = new LinkedHashMap<>();
        source.forEach((rawKey, value) -> {
            String key = String.valueOf(rawKey);
            if (isSensitiveKey(key)) {
                sanitized.put(key, "[REDACTED]");
            } else {
                sanitized.put(key, sanitizeValue(value));
            }
        });
        return sanitized;
    }

    private static Object sanitizeValue(Object value) {
        if (value instanceof Map<?, ?> nestedMap) {
            return sanitizeMap(nestedMap);
        }
        if (value instanceof List<?> list) {
            return list.stream().map(LogSanitizer::sanitizeValue).toList();
        }
        if (value instanceof String text) {
            return SENSITIVE_MESSAGE.matcher(text).replaceAll("$1$2[REDACTED]");
        }
        return value;
    }

    private static boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("-", "_");
        return SENSITIVE_KEY_PARTS.stream().anyMatch(normalized::contains);
    }
}

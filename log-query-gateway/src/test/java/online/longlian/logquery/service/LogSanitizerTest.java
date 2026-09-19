package online.longlian.logquery.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class LogSanitizerTest {

    private final LogSanitizer sanitizer = new LogSanitizer();

    @Test
    void redactsSensitiveFieldsAndMessageFragments() {
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("user", "alice");
        source.put("access_token", "do-not-return");
        source.put("_msg", "login password=secret123 user=alice");

        Map<String, Object> result = sanitizer.sanitize(source);

        assertThat(result).containsEntry("user", "alice");
        assertThat(result).containsEntry("access_token", "[REDACTED]");
        assertThat(result.get("_msg")).isEqualTo("login password=[REDACTED] user=alice");
    }
}

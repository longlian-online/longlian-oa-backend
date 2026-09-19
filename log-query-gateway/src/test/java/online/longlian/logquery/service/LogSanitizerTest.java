package online.longlian.logquery.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
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

    @Test
    void redactsNestedFieldsAndBodyFragments() {
        Map<String, Object> nested = new LinkedHashMap<>();
        nested.put("authorization", "Bearer do-not-return");
        nested.put("body", "request password=secret123 private_key=secret456");

        Map<String, Object> source = new LinkedHashMap<>();
        source.put("context", nested);
        source.put("events", List.of(Map.of("message", "token=secret456")));

        Map<String, Object> result = sanitizer.sanitize(source);

        assertThat(result).extractingByKey("context")
                .isInstanceOf(Map.class)
                .satisfies(value -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> sanitizedContext = (Map<String, Object>) value;
                    assertThat(sanitizedContext).containsEntry("authorization", "[REDACTED]");
                    assertThat(sanitizedContext).containsEntry("body",
                            "request password=[REDACTED] private_key=[REDACTED]");
                });
        assertThat(result).extractingByKey("events")
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .first()
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("message", "token=[REDACTED]");
    }
}

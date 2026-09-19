package online.longlian.logquery.api;

import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LogSearchRequest(
        @NotBlank(message = "service is required")
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,99}", message = "service has invalid format")
        String service,

        @NotBlank(message = "environment is required")
        @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,31}", message = "environment has invalid format")
        String environment,

        @Size(max = 200, message = "keyword is too long")
        String keyword,

        @Pattern(regexp = "[0-9a-fA-F]{16,64}", message = "traceId has invalid format")
        String traceId,

        List<@Pattern(regexp = "TRACE|DEBUG|INFO|WARN|ERROR|FATAL", message = "level has invalid value") String> levels,

        @Pattern(regexp = "[1-9][0-9]*(ms|s|m|h|d)", message = "since has invalid format")
        String since,

        Instant from,
        Instant to,

        @Min(value = 1, message = "limit must be at least 1")
        @Max(value = 200, message = "limit must be at most 200")
        Integer limit
) {
}

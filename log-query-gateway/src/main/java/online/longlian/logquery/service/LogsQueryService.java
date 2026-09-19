package online.longlian.logquery.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import online.longlian.logquery.api.LogSearchRequest;
import online.longlian.logquery.api.LogSearchResponse;
import online.longlian.logquery.config.LogQueryGatewayProperties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class LogsQueryService {

    private static final Logger log = LoggerFactory.getLogger(LogsQueryService.class);

    private final LogsQlQueryBuilder queryBuilder;
    private final VictoriaLogsClient victoriaLogsClient;
    private final LogSanitizer sanitizer;
    private final LogQueryGatewayProperties properties;
    private final Semaphore concurrency;

    public LogsQueryService(LogsQlQueryBuilder queryBuilder, VictoriaLogsClient victoriaLogsClient,
                            LogSanitizer sanitizer, LogQueryGatewayProperties properties) {
        this.queryBuilder = queryBuilder;
        this.victoriaLogsClient = victoriaLogsClient;
        this.sanitizer = sanitizer;
        this.properties = properties;
        this.concurrency = new Semaphore(properties.getQuery().getMaxConcurrent());
    }

    public LogSearchResponse search(LogSearchRequest request, String requestId) {
        TimeRange range = resolveTimeRange(request);
        int limit = resolveLimit(request.limit());
        String query = queryBuilder.build(request);
        boolean acquired = concurrency.tryAcquire();
        if (!acquired) {
            throw new LogQueryException(429, "too many concurrent log queries");
        }

        long started = System.nanoTime();
        try {
            List<Map<String, Object>> records = victoriaLogsClient.query(
                    query, range.from().toString(), range.to().toString(), limit);
            List<Map<String, Object>> sanitized = records.stream().map(sanitizer::sanitize).toList();
            long tookMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            log.info("log query completed requestId={} service={} environment={} resultCount={} tookMs={}",
                    requestId, request.service(), request.environment(), sanitized.size(), tookMs);
            return new LogSearchResponse(requestId, tookMs, sanitized.size(), sanitized);
        } finally {
            concurrency.release();
        }
    }

    private TimeRange resolveTimeRange(LogSearchRequest request) {
        if (request.from() != null || request.to() != null) {
            if (request.since() != null && !request.since().isBlank()) {
                throw new LogQueryException(400, "since cannot be combined with from and to");
            }
            if (request.from() == null || request.to() == null || !request.from().isBefore(request.to())) {
                throw new LogQueryException(400, "from and to must define a positive time range");
            }
            Duration range = Duration.between(request.from(), request.to());
            if (range.compareTo(properties.getQuery().getMaxRange()) > 0) {
                throw new LogQueryException(400, "time range exceeds configured maximum");
            }
            return new TimeRange(request.from(), request.to());
        }

        Duration duration = request.since() == null || request.since().isBlank()
                ? properties.getQuery().getDefaultRange()
                : parseDuration(request.since());
        if (duration.isZero() || duration.isNegative() || duration.compareTo(properties.getQuery().getMaxRange()) > 0) {
            throw new LogQueryException(400, "since exceeds configured maximum");
        }
        Instant to = Instant.now();
        return new TimeRange(to.minus(duration), to);
    }

    private int resolveLimit(Integer requested) {
        int limit = requested == null ? properties.getQuery().getMaxLimit() : requested;
        if (limit < 1 || limit > properties.getQuery().getMaxLimit()) {
            throw new LogQueryException(400, "limit exceeds configured maximum");
        }
        return limit;
    }

    private static Duration parseDuration(String value) {
        String number;
        String unit;
        if (value.endsWith("ms")) {
            number = value.substring(0, value.length() - 2);
            unit = "ms";
        } else {
            number = value.substring(0, value.length() - 1);
            unit = value.substring(value.length() - 1);
        }
        long amount;
        try {
            amount = Long.parseLong(number);
        } catch (NumberFormatException exception) {
            throw new LogQueryException(400, "since has invalid format", exception);
        }
        return switch (unit) {
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            case "d" -> Duration.ofDays(amount);
            default -> throw new LogQueryException(400, "since has invalid format");
        };
    }

    private record TimeRange(Instant from, Instant to) {
    }
}

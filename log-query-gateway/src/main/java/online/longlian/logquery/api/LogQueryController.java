package online.longlian.logquery.api;

import java.util.UUID;

import jakarta.validation.Valid;

import online.longlian.logquery.service.LogsQueryService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/logs")
public class LogQueryController {

    private final LogsQueryService logsQueryService;

    public LogQueryController(LogsQueryService logsQueryService) {
        this.logsQueryService = logsQueryService;
    }

    @PostMapping("/search")
    public ResponseEntity<LogSearchResponse> search(
            @Valid @RequestBody LogSearchRequest request,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId) {
        String resolvedRequestId = requestId == null || requestId.isBlank()
                ? UUID.randomUUID().toString()
                : requestId;
        return ResponseEntity.ok(logsQueryService.search(request, resolvedRequestId));
    }
}

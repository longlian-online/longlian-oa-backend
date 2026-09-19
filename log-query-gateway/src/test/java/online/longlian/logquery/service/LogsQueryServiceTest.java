package online.longlian.logquery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import online.longlian.logquery.api.LogSearchRequest;
import online.longlian.logquery.api.LogSearchResponse;
import online.longlian.logquery.config.LogQueryGatewayProperties;

import org.junit.jupiter.api.Test;

class LogsQueryServiceTest {

    @Test
    void appliesTimeAndLimitPolicyAndSanitizesResults() {
        VictoriaLogsClient client = mock(VictoriaLogsClient.class);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("_msg", "token=secret");
        when(client.query(anyString(), anyString(), anyString(), anyInt())).thenReturn(List.of(record));

        LogsQueryService service = new LogsQueryService(
                new LogsQlQueryBuilder(), client, new LogSanitizer(), new LogQueryGatewayProperties());
        LogSearchRequest request = new LogSearchRequest(
                "longlian-oa", "prod", null, null, null, "10ms", null, null, 20);

        LogSearchResponse response = service.search(request, "request-1");

        assertThat(response.requestId()).isEqualTo("request-1");
        assertThat(response.total()).isOne();
        assertThat(response.logs().getFirst().get("_msg")).isEqualTo("token=[REDACTED]");
        verify(client).query(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.eq(20));
    }

    @Test
    void rejectsMixedRelativeAndAbsoluteTimeRange() {
        LogsQueryService service = new LogsQueryService(
                new LogsQlQueryBuilder(), mock(VictoriaLogsClient.class), new LogSanitizer(),
                new LogQueryGatewayProperties());
        LogSearchRequest request = new LogSearchRequest(
                "longlian-oa", "prod", null, null, null, "15m",
                java.time.Instant.parse("2026-09-19T00:00:00Z"),
                java.time.Instant.parse("2026-09-19T00:01:00Z"), null);

        assertThatThrownBy(() -> service.search(request, "request-1"))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("since cannot be combined with from and to");
    }
}

package online.longlian.logquery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import online.longlian.logquery.api.LogSearchRequest;

import org.junit.jupiter.api.Test;

class LogsQlQueryBuilderTest {

    private final LogsQlQueryBuilder builder = new LogsQlQueryBuilder();

    @Test
    void buildsScopedQueryWithOptionalFilters() {
        LogSearchRequest request = new LogSearchRequest(
                "longlian-oa", "prod", "database timeout", "0123456789abcdef",
                List.of("ERROR", "WARN"), "15m", null, null, 50);

        String query = builder.build(request);

        assertThat(query).isEqualTo(
                "{service.name=\"longlian-oa\",deployment.environment=\"prod\"} "
                        + "\"database timeout\" trace_id:=0123456789abcdef "
                        + "(severity_text:ERROR OR severity_text:WARN)");
    }

    @Test
    void rejectsUnsafeStreamValueBeforeSendingToVictoriaLogs() {
        LogSearchRequest request = new LogSearchRequest(
                "prod\" OR _stream:*=~\"", "prod", null, null, null, "15m", null, null, null);

        assertThatThrownBy(() -> builder.build(request))
                .isInstanceOf(LogQueryException.class)
                .hasMessage("service has invalid format");
    }
}

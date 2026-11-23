package online.longlian.app.common.metrics;

import org.junit.jupiter.api.Test;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import static org.assertj.core.api.Assertions.assertThat;

class ApiResultMetricsTest {

    @Test
    void shouldUseRouteTemplateInsteadOfRawUri() {
        try (ResultMetricCapture capture = ResultMetricCapture.open()) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/app/projects/99");
            request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, new Object() {
                @Override
                public String toString() {
                    return "/app/projects/{projectId}";
                }
            });

            capture.metrics().record(request, 0);

            assertThat(capture.count(0, "/app/projects/{projectId}")).isEqualTo(1);
            assertThat(capture.count(0, "/app/projects/99")).isZero();
        }
    }

    @Test
    void shouldUseNoneWhenRouteIsMissingOrBlank() {
        try (ResultMetricCapture capture = ResultMetricCapture.open()) {
            MockHttpServletRequest missing = new MockHttpServletRequest("GET", "/app/projects/99");
            capture.metrics().record(missing, 8);

            MockHttpServletRequest blank = new MockHttpServletRequest();
            blank.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "  ");
            capture.metrics().record(blank, 8);

            ServerHttpRequest unmatched = null;
            capture.metrics().record(unmatched, 1);

            assertThat(capture.count(8, ApiResultMetrics.UNMATCHED_ROUTE)).isEqualTo(2);
            assertThat(capture.count(1, ApiResultMetrics.UNMATCHED_ROUTE)).isEqualTo(1);
            assertThat(capture.count(8, "/app/projects/99")).isZero();
        }
    }
}

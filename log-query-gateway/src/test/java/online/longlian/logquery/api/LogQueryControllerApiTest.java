package online.longlian.logquery.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import online.longlian.logquery.service.LogsQueryService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(LogQueryController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GatewayExceptionHandler.class)
class LogQueryControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LogsQueryService logsQueryService;

    @Test
    void searchesLogsWithValidatedRequest() throws Exception {
        when(logsQueryService.search(any(), eq("request-1")))
                .thenReturn(new LogSearchResponse("request-1", 4, 1, List.of(java.util.Map.of("_msg", "ok"))));

        mockMvc.perform(post("/api/v1/logs/search")
                        .header("X-Request-Id", "request-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service":"longlian-oa","environment":"prod","since":"15m","limit":10}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value("request-1"))
                .andExpect(jsonPath("$.total").value(1));

        verify(logsQueryService).search(any(), eq("request-1"));
    }

    @Test
    void rejectsInvalidServiceBeforeCallingService() throws Exception {
        mockMvc.perform(post("/api/v1/logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service":"bad service","environment":"prod","since":"15m"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void rejectsInvalidInstantAsBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/logs/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"service":"longlian-oa","environment":"prod","from":"not-an-instant"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("request body is invalid"));
    }
}

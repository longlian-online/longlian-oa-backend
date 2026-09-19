package online.longlian.logquery.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import online.longlian.logquery.config.LogQueryGatewayProperties;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class VictoriaLogsClient {

    private final RestClient restClient;
    private final LogQueryGatewayProperties properties;
    private final ObjectMapper objectMapper;

    public VictoriaLogsClient(RestClient restClient, LogQueryGatewayProperties properties, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> query(String query, String start, String end, int limit) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("query", query);
        form.add("start", start);
        form.add("end", end);
        form.add("limit", String.valueOf(limit));
        try {
            String body = restClient.post()
                    .uri(properties.getVictoriaLogs().getQueryUrl())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(String.class);
            return parseNdjson(body);
        } catch (RestClientException exception) {
            throw new LogQueryException(502, "VictoriaLogs query failed", exception);
        }
    }

    private List<Map<String, Object>> parseNdjson(String body) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (body == null || body.isBlank()) {
            return result;
        }
        for (String line : body.split("\\R")) {
            if (line.isBlank()) {
                continue;
            }
            try {
                Map<String, Object> record = objectMapper.readValue(line,
                        new TypeReference<LinkedHashMap<String, Object>>() {
                        });
                result.add(record);
            } catch (JsonProcessingException exception) {
                throw new LogQueryException(502, "VictoriaLogs returned invalid NDJSON", exception);
            }
        }
        return result;
    }
}

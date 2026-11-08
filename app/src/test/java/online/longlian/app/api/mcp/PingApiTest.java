package online.longlian.app.api.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class PingApiTest extends BaseApiTest {

    private static final String ENDPOINT = "/mcp";
    private static final String ACCEPT = "application/json, text/event-stream";
    private static final String PROTOCOL_VERSION = "2025-03-26";
    private final List<Session> sessions = new ArrayList<>();

    @AfterEach
    void closeSessions() {
        try {
            for (Session session : sessions) {
                sessionRequest(session).delete(ENDPOINT).then().statusCode(200);
            }
        } finally {
            sessions.clear();
        }
    }

    // 没有组织的普通用户也能初始化 MCP，响应必须是原生 JSON-RPC 而不是 REST Result。
    @Test
    void shouldInitializeWithoutOrganizationAndWithoutRestWrapper() {
        String token = createUserToken();
        Response response = initialize(token);
        JSONObject envelope = rpcResponse(response, 1);
        JSONObject result = envelope.getJSONObject("result");

        assertThat(result.getString("protocolVersion")).isEqualTo(PROTOCOL_VERSION);
        assertThat(result.getJSONObject("serverInfo").getString("name")).isEqualTo("longlian-oa");
        assertThat(result.getJSONObject("capabilities")).containsKey("tools");
        assertThat(response.header("Mcp-Session-Id")).isNotBlank();
        sessions.add(new Session(token, response.header("Mcp-Session-Id"), PROTOCOL_VERSION));
    }

    // ping 保持无参；任务工具上线后也不能混入正式提交或管理操作。
    @Test
    void shouldListToolsAndKeepPingWithoutArguments() {
        Session session = createSession();
        JSONObject result = rpcResponse(send(session, 2, "tools/list", Map.of()), 2)
                .getJSONObject("result");

        assertThat(result.getJSONArray("tools").stream()
                .map(value -> ((JSONObject) value).getString("name")))
                .containsExactlyInAnyOrder("ping", "find_projects", "list_project_items", "get_project_tasks",
                        "get_task", "claim_task", "get_task_file_url", "create_task_upload", "save_task_draft",
                        "list_my_tasks", "list_claimable_tasks", "abandon_task");
        JSONObject tool = result.getJSONArray("tools").stream().map(value -> (JSONObject) value)
                .filter(value -> "ping".equals(value.getString("name"))).findFirst().orElseThrow();
        assertThat(tool.getString("name")).isEqualTo("ping");
        JSONObject schema = tool.getJSONObject("inputSchema");
        assertThat(schema.getString("type")).isEqualTo("object");
        assertThat(schema.getJSONObject("properties")).isEmpty();
        if (schema.containsKey("required")) {
            assertThat(schema.getJSONArray("required")).isEmpty();
        }
    }

    // 空参数调用工具必须得到文本 pong，不能被统一响应增强包装成 Result。
    @Test
    void shouldCallPingAndReturnPongWithoutRestWrapper() {
        Session session = createSession();
        JSONObject result = rpcResponse(send(session, 2, "tools/call",
                Map.of("name", "ping", "arguments", Map.of())), 2).getJSONObject("result");

        assertThat(result.getBoolean("isError")).isNotEqualTo(Boolean.TRUE);
        assertThat(result.getJSONArray("content")).hasSize(1);
        JSONObject content = result.getJSONArray("content").getJSONObject(0);
        assertThat(content.getString("type")).isEqualTo("text");
        assertThat(content.getString("text")).isEqualTo("pong");
        assertThat(result).doesNotContainKeys("code", "msg", "data");
    }

    // JSON-RPC ping 是协议探活，只返回空对象，不能误路由为返回 pong 的同名工具。
    @Test
    void shouldKeepProtocolPingSeparateFromToolPing() {
        Session session = createSession();
        JSONObject envelope = rpcResponse(send(session, 2, "ping", Map.of()), 2);

        assertThat(envelope.getJSONObject("result")).isEmpty();
        JSONObject toolResult = rpcResponse(send(session, 3, "tools/call",
                Map.of("name", "ping", "arguments", Map.of())), 3).getJSONObject("result");
        assertThat(toolResult.getJSONArray("content").getJSONObject(0).getString("text"))
                .isEqualTo("pong");
    }

    // MCP 使用协议层 HTTP 401，不受 REST 资源查找和响应包装影响。
    @Test
    void shouldRejectInitializeWithoutToken() {
        request().accept(ACCEPT).body(initializeRequest()).post(ENDPOINT)
                .then().statusCode(401).body(equalTo(""));
    }

    // 无效 Bearer 令牌不能产生 MCP 会话。
    @Test
    void shouldRejectInitializeWithInvalidToken() {
        authRequest("invalid-token").accept(ACCEPT).body(initializeRequest()).post(ENDPOINT)
                .then().statusCode(401).body(equalTo(""));
    }

    // 已建立的 MCP 会话不能代替用户凭据，每次工具调用都必须认证。
    @Test
    void shouldRejectToolCallWithSessionButWithoutToken() {
        Session session = createSession();
        request().accept(ACCEPT)
                .header("Mcp-Session-Id", session.id())
                .header("MCP-Protocol-Version", session.protocolVersion())
                .body(Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/call",
                        "params", Map.of("name", "ping", "arguments", Map.of())))
                .post(ENDPOINT).then().statusCode(401).body(equalTo(""));
    }

    // 即使会话有效，无效令牌也不能继续执行工具。
    @Test
    void shouldRejectToolCallWithSessionAndInvalidToken() {
        Session session = createSession();
        authRequest("invalid-token").accept(ACCEPT)
                .header("Mcp-Session-Id", session.id())
                .header("MCP-Protocol-Version", session.protocolVersion())
                .body(Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/call",
                        "params", Map.of("name", "ping", "arguments", Map.of())))
                .post(ENDPOINT).then().statusCode(401).body(equalTo(""));
    }

    private String createUserToken() {
        createTestUser(90001L, "mcp_user", "123456", "mcp_user@example.com");
        return loginAs("mcp_user", "123456");
    }

    private Map<String, Object> initializeRequest() {
        return Map.of("jsonrpc", "2.0", "id", 1, "method", "initialize", "params", Map.of(
                "protocolVersion", PROTOCOL_VERSION,
                "capabilities", Map.of(),
                "clientInfo", Map.of("name", "ping-api-test", "version", "1.0")));
    }

    private Response initialize(String token) {
        return authRequest(token).accept(ACCEPT).body(initializeRequest()).post(ENDPOINT);
    }

    private Session createSession() {
        String token = createUserToken();
        Response response = initialize(token);
        JSONObject result = rpcResponse(response, 1).getJSONObject("result");
        String sessionId = response.header("Mcp-Session-Id");
        assertThat(sessionId).isNotBlank();
        assertThat(result.getString("protocolVersion")).isEqualTo(PROTOCOL_VERSION);
        Session session = new Session(token, sessionId, result.getString("protocolVersion"));
        sessions.add(session);
        sessionRequest(session)
                .body(Map.of("jsonrpc", "2.0", "method", "notifications/initialized"))
                .post(ENDPOINT).then().statusCode(202);
        return session;
    }

    private RequestSpecification sessionRequest(Session session) {
        return authRequest(session.token()).accept(ACCEPT)
                .header("Mcp-Session-Id", session.id())
                .header("MCP-Protocol-Version", session.protocolVersion());
    }

    private Response send(Session session, int id, String method, Map<String, Object> params) {
        return sessionRequest(session)
                .body(Map.of("jsonrpc", "2.0", "id", id, "method", method, "params", params))
                .post(ENDPOINT);
    }

    private JSONObject rpcResponse(Response response, int id) {
        response.then().statusCode(200);
        String body = response.asString();
        JSONObject envelope;
        if (response.contentType().startsWith("text/event-stream")) {
            // Streamable HTTP 可返回 SSE；按事件拼接 data 行，不把 SSE 外壳当 JSON 解析。
            envelope = null;
            for (String event : body.replace("\r\n", "\n").split("\n\n")) {
                String data = event.lines().filter(line -> line.startsWith("data:"))
                        .map(line -> line.substring(5).stripLeading()).collect(Collectors.joining("\n"));
                if (!data.isBlank()) {
                    JSONObject candidate = JSON.parseObject(data);
                    if (Integer.valueOf(id).equals(candidate.getInteger("id"))) {
                        envelope = candidate;
                        break;
                    }
                }
            }
        } else {
            assertThat(response.contentType()).startsWith("application/json");
            envelope = JSON.parseObject(body);
        }
        assertThat(envelope).as("JSON-RPC response for id %s: %s", id, body).isNotNull();
        assertThat(envelope.getString("jsonrpc")).isEqualTo("2.0");
        assertThat(envelope.getInteger("id")).isEqualTo(id);
        assertThat(envelope).containsKey("result").doesNotContainKeys("error", "code", "msg", "data");
        return envelope;
    }

    private record Session(String token, String id, String protocolVersion) {
    }
}

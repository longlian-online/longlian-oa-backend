package online.longlian.app.api.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.filter.JwtAuthenticationFilter;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyParamsBO;
import online.longlian.app.service.app.UserOrgApiKeyService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

class ApiKeyApiTest extends BaseApiTest {

    private static final String PATH = "/app/user/organizations/1/api-keys";
    private static final String ACCEPT = "application/json, text/event-stream";
    private final List<Session> sessions = new ArrayList<>();
    @Autowired private UserOrgApiKeyService apiKeyService;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private FilterChainProxy springSecurityFilterChain;
    @Autowired private FilterRegistrationBean<JwtAuthenticationFilter> jwtFilterRegistration;

    @Test
    void shouldUseExistingSecurityChainForApiKeyAndJwt() {
        var request = new MockHttpServletRequest("POST", "/mcp");
        request.setServletPath("/mcp");
        request.addHeader("Authorization", "Bearer ll_mcp_key");
        var apiKeyChain = springSecurityFilterChain.getFilterChains().stream()
                .filter(chain -> chain.matches(request)).findFirst().orElseThrow();
        assertThat(apiKeyChain.getFilters().stream().filter(filter -> filter instanceof JwtAuthenticationFilter)).hasSize(1);
        assertThat(jwtFilterRegistration.isEnabled()).isFalse();

        request.removeHeader("Authorization");
        request.addHeader("Authorization", "Bearer jwt-token");
        var jwtChain = springSecurityFilterChain.getFilterChains().stream()
                .filter(chain -> chain.matches(request)).findFirst().orElseThrow();
        assertThat(jwtChain).isSameAs(apiKeyChain);
        assertThat(jwtChain.getFilters().stream().filter(filter -> filter instanceof JwtAuthenticationFilter)).hasSize(1);

        request.removeHeader("Authorization");
        assertThat(springSecurityFilterChain.getFilterChains().stream()
                .filter(chain -> chain.matches(request)).findFirst().orElseThrow()).isSameAs(jwtChain);

        request.addHeader("Authorization", "Bearer ll_mcp_key");
        request.setServletPath("/app/user/");
        assertThat(springSecurityFilterChain.getFilterChains().stream()
                .filter(chain -> chain.matches(request)).findFirst().orElseThrow()).isSameAs(jwtChain);
    }

    @AfterEach
    void closeSessions() {
        for (Session session : sessions) {
            int status = userRequest(session.key()).header("Mcp-Session-Id", session.id()).delete("/mcp").statusCode();
            assertThat(status).isIn(200, 401, 403);
        }
        sessions.clear();
    }

    // 原文仅生成时返回；替换当前摘要后，已有会话也不能凭旧 Key 发起新请求。
    @Test
    void shouldGenerateRotateAndRevokeWithoutPersistingPlaintext() {
        String token = fixture();
        userRequest(token).get(PATH + "/current").then().body("data.state", equalTo("NOT_CREATED"));
        String first = generate(token, 1);
        assertThat(first).matches("ll_mcp_[A-Za-z0-9_-]{43}");
        String digest = jdbcTemplate.queryForObject("SELECT key_hash FROM user_org_api_key", String.class);
        assertThat(digest).hasSize(64).isNotEqualTo(first);
        Response metadata = userRequest(token).get(PATH + "/current");
        metadata.then().body("data.state", equalTo("ACTIVE")).header("Cache-Control", "no-store");
        assertThat(metadata.asString()).doesNotContain(first, digest, "apiKey", "keyHash");
        Session session = connect(first);
        assertThat(tool(session, "find_projects").getJSONArray("list").getJSONObject(0).getString("title")).isEqualTo("甲企划");
        String second = generate(token, 1);
        assertThat(second).isNotEqualTo(first);
        userRequest(first).accept(ACCEPT).header("Mcp-Session-Id", session.id()).body(call("ping"))
                .post("/mcp").then().statusCode(401);
        connect(second);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_org_api_key", Integer.class)).isEqualTo(1);
        userRequest(token).delete(PATH + "/current").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(token).delete(PATH + "/current").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(token).get(PATH + "/current").then().body("data.state", equalTo("REVOKED"));
        initialize(second).then().statusCode(401);
    }

    // Key 固定绑定用户及组织，不能用组织头切换；重置不影响其他组织或同组织的其他用户。
    @Test
    void shouldIsolateOrganizationsAndUsers() {
        String token = fixture();
        String first = generate(token, 1);
        String otherOrg = generate(token, 2);
        createTestUser(2L, "second", "123456", "second@example.com");
        createOrganizationMember(3L, 1L, 2L, "ORG_USER");
        String otherUser = generate(loginAs("second", "123456"), 1);
        assertThat(tool(connect(otherOrg), "find_projects").getJSONArray("list").getJSONObject(0).getString("title")).isEqualTo("乙企划");
        userRequest(first).header("X-Org-Id", "2").accept(ACCEPT).body(initialization()).post("/mcp").then().statusCode(403);
        generate(token, 1);
        initialize(first).then().statusCode(401);
        connect(otherOrg);
        connect(otherUser);
        userRequest(otherOrg).post(PATH).then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        userRequest(otherUser).get("/app/user/").then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    // 同一个用户在不同组织的凭据不得污染用户缓存或沿用上次 MCP 请求的组织范围。
    @Test
    void shouldResolveOrganizationFromEachRequestInsteadOfCachedUserOrSession() {
        String token = fixture();
        String firstKey = generate(token, 1);
        String secondKey = generate(token, 2);
        Session session = connect(firstKey);
        assertThat(tool(session, "find_projects").getJSONArray("list").getJSONObject(0).getString("title")).isEqualTo("甲企划");
        assertThat(tool(new Session(secondKey, session.id()), "find_projects").getJSONArray("list").getJSONObject(0)
                .getString("title")).isEqualTo("乙企划");
        assertThat(tool(session, "find_projects").getJSONArray("list").getJSONObject(0).getString("title")).isEqualTo("甲企划");
        userRequest(token).get("/app/user/").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    // 成员禁用只暂停，重新启用可恢复；禁用期间仍能查看并永久撤销自己的 Key。
    @Test
    void shouldSuspendAndRestoreMembershipWithoutResurrectingRevokedKey() {
        String token = fixture();
        String key = generate(token, 1);
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE id = 1");
        initialize(key).then().statusCode(403);
        userRequest(token).get(PATH + "/current").then().body("data.state", equalTo("SUSPENDED"))
                .body("data.canGenerate", equalTo(false)).body("data.canRevoke", equalTo(true));
        userRequest(token).get("/app/user/organizations/api-key-settings").then().body("data.size()", equalTo(2));
        userRequest(token).post(PATH).then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status = 1 WHERE id = 1");
        connect(key);
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE id = 1");
        userRequest(token).delete(PATH + "/current").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status = 1 WHERE id = 1");
        initialize(key).then().statusCode(401);
    }

    // 组织与全局用户状态每次读取数据库，不依赖可能已经命中的 JWT 登录缓存。
    @Test
    void shouldValidateOrganizationAndUserStateOnEveryRequest() {
        String token = fixture();
        String key = generate(token, 1);
        jdbcTemplate.update("UPDATE organization SET status = 0 WHERE id = 1");
        initialize(key).then().statusCode(403);
        userRequest(token).get(PATH + "/current").then().body("data.state", equalTo("SUSPENDED"));
        jdbcTemplate.update("UPDATE organization SET status = 1 WHERE id = 1");
        connect(key);
        jdbcTemplate.update("UPDATE `user` SET status = 0 WHERE id = 1");
        initialize(key).then().statusCode(401);
        jdbcTemplate.update("UPDATE `user` SET status = 1 WHERE id = 1");
        jdbcTemplate.update("UPDATE organization_member SET deleted_at = NOW() WHERE id = 1");
        initialize(key).then().statusCode(401);
    }

    // 密码变更沿用原事务，同时撤销本人所有组织 Key。
    @Test
    void shouldRevokeAllOwnKeysWhenPasswordChanges() {
        String token = fixture();
        String first = generate(token, 1);
        String second = generate(token, 2);
        userRequest(token).body(Map.of("oldPassword", "123456", "newPassword", "Newpass123!"))
                .patch("/app/user/password").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        initialize(first).then().statusCode(401);
        initialize(second).then().statusCode(401);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_org_api_key WHERE key_hash IS NOT NULL", Integer.class)).isZero();
    }

    // 无登录、无成员关系和非法路径参数均拒绝，不因默认组织头改变管理目标。
    @Test
    void shouldRejectInvalidManagementRequests() {
        String token = fixture();
        request().post(PATH).then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        userRequest(token).post("/app/user/organizations/-1/api-keys").then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        userRequest(token).get("/app/user/organizations/0/api-keys/current").then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        userRequest(token).delete("/app/user/organizations/-1/api-keys/current").then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        userRequest(token).get("/app/user/organizations/invalid/api-keys/current").then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        createOrganization(3L, "未加入组织");
        userRequest(token).post("/app/user/organizations/3/api-keys").then().body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        initialize("ll_mcp_invalid").then().statusCode(401);
        userRequest(token).get(PATH + "/current").then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_org_api_key", Integer.class)).isZero();
    }

    // 生成事务回滚后旧 Key 保持可用；退出流程的撤销参与调用方事务，恢复成员也不会复活旧 Key。
    @Test
    void shouldRollbackRotationAndKeepMembershipRevocationPermanent() {
        String token = fixture();
        String original = generate(token, 1);
        AtomicReference<String> rolledBack = new AtomicReference<>();
        assertThatThrownBy(() -> new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            rolledBack.set(apiKeyService.generate(new UserOrgApiKeyParamsBO(1L, 1L)).apiKey());
            throw new IllegalStateException("模拟调用方回滚");
        })).isInstanceOf(IllegalStateException.class);
        connect(original);
        initialize(rolledBack.get()).then().statusCode(401);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            apiKeyService.revokeForMembership(new UserOrgApiKeyParamsBO(1L, 1L));
            jdbcTemplate.update("UPDATE organization_member SET deleted_at = NOW() WHERE id = 1");
        });
        jdbcTemplate.update("UPDATE organization_member SET deleted_at = NULL WHERE id = 1");
        initialize(original).then().statusCode(401);
    }

    private String fixture() {
        createUserWithOrganization(1L, "keyowner", "123456", "keyowner@example.com", 1L, 1L, "ORG_USER");
        createOrganization(2L, "乙组织");
        createOrganizationMember(2L, 2L, 1L, "ORG_USER");
        for (int id = 1; id <= 2; id++) {
            jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (?, ?, '类型', 1, 1)", id, id);
            jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, cover_file_id, description, status, creator_id) "
                    + "VALUES (?, ?, ?, ?, 'keys', '{}', 0, '测试', 1, 1)", id, id, id, id == 1 ? "甲企划" : "乙企划");
        }
        return loginAs("keyowner", "123456");
    }

    private String generate(String token, int orgId) {
        return userRequest(token).post("/app/user/organizations/" + orgId + "/api-keys").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).extract().path("data.apiKey");
    }

    private Map<String, Object> initialization() {
        return Map.of("jsonrpc", "2.0", "id", 1, "method", "initialize", "params", Map.of(
                "protocolVersion", "2025-03-26", "capabilities", Map.of(), "clientInfo", Map.of("name", "api-key-test", "version", "1.0")));
    }

    private Response initialize(String key) {
        return userRequest(key).accept(ACCEPT).body(initialization()).post("/mcp");
    }

    private Session connect(String key) {
        Response response = initialize(key);
        response.then().statusCode(200);
        Session session = new Session(key, response.header("Mcp-Session-Id"));
        sessions.add(session);
        userRequest(key).accept(ACCEPT).header("Mcp-Session-Id", session.id())
                .body(Map.of("jsonrpc", "2.0", "method", "notifications/initialized")).post("/mcp").then().statusCode(202);
        return session;
    }

    private Map<String, Object> call(String tool) {
        return Map.of("jsonrpc", "2.0", "id", 2, "method", "tools/call", "params", Map.of("name", tool, "arguments", Map.of()));
    }

    private JSONObject tool(Session session, String tool) {
        Response response = userRequest(session.key()).accept(ACCEPT).header("Mcp-Session-Id", session.id())
                .body(call(tool)).post("/mcp");
        response.then().statusCode(200);
        String body = response.asString().replace("\r\n", "\n");
        if (response.contentType().startsWith("text/event-stream")) {
            body = body.lines().filter(line -> line.startsWith("data:")).map(line -> line.substring(5).stripLeading())
                    .collect(Collectors.joining("\n"));
        }
        JSONObject result = JSON.parseObject(body).getJSONObject("result");
        assertThat(result.getBoolean("isError")).isNotEqualTo(true);
        return JSON.parseObject(result.getJSONArray("content").getJSONObject(0).getString("text"));
    }

    private record Session(String key, String id) { }
}

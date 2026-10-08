package online.longlian.app.api.app;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.interceptor.OrganizationScopeInterceptor;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

public class OrganizationScopeApiTest extends BaseApiTest {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    /**
     * 同一个 token 按请求头写入各自的组织，并行时也不串组织。
     */
    @Test
    void shouldWriteEachHeaderOrgWithTheSameToken() throws Exception {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 1L);
        String token = loginAs("admin", "123456");

        authRequest(token, 1L).body(Map.of("name", "类型A")).post("/orgadmin/project-types")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token, 2L).body(Map.of("name", "类型B")).post("/orgadmin/project-types")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(orgIdOfProjectType("类型A")).isEqualTo(1L);
        assertThat(orgIdOfProjectType("类型B")).isEqualTo(2L);

        jdbcTemplate.update("DELETE FROM project_type");
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            CompletableFuture<Response> first = CompletableFuture.supplyAsync(() ->
                    authRequest(token, 1L).body(Map.of("name", "类型A")).post("/orgadmin/project-types"), executor);
            CompletableFuture<Response> second = CompletableFuture.supplyAsync(() ->
                    authRequest(token, 2L).body(Map.of("name", "类型B")).post("/orgadmin/project-types"), executor);
            first.get(15, TimeUnit.SECONDS).then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
            second.get(15, TimeUnit.SECONDS).then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        }
        assertThat(orgIdOfProjectType("类型A")).isEqualTo(1L);
        assertThat(orgIdOfProjectType("类型B")).isEqualTo(2L);
        assertThat(redisTemplate.hasKey("currentOrg:user:1")).isNotEqualTo(Boolean.TRUE);
    }

    /**
     * 角色跟这次请求头走，登录时组织 1 的管理员身份不能带到组织 2。
     */
    @Test
    void shouldAuthorizeByHeaderRoleInsteadOfLoginSuggestion() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_USER");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 1L);
        String token = loginAs("admin", "123456");

        authRequest(token, 2L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token, 1L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 管理员令牌不是用户令牌，不能把令牌类型错误误报为组织为空。
     */
    @Test
    void shouldRejectAdminTokenAsUnauthorized() {
        createAdmin(1L, "system_admin", "123456", "root");
        String token = adminLoginAs("system_admin", "123456");

        userRequest(token)
                .header(OrganizationScopeInterceptor.ORG_ID_HEADER, "1")
                .get("/app/projects")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 缺头或头非法时失败，不回退到 default_org_id。
     */
    @Test
    void shouldFailClosedInsteadOfFallingBackToDefaultOrg() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(3L, "其他组织");
        createOrganization(4L, "禁用组织");
        jdbcTemplate.update("UPDATE organization SET status = 0 WHERE id = 4");
        createOrganizationMember(4L, 4L, 1L, "ORG_ADMIN");
        createOrganization(5L, "成员禁用组织");
        createOrganizationMember(5L, 5L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE organization_member SET status = 0 WHERE id = 5");
        String token = loginAs("admin", "123456");

        assertOperationFail(userRequest(token).get("/app/projects"));
        for (String header : new String[]{"", "0", "-1", "abc"}) {
            assertOperationFail(userRequest(token).header(OrganizationScopeInterceptor.ORG_ID_HEADER, header).get("/app/projects"));
        }
        assertOperationFail(authRequest(token, 3L).get("/app/projects"));
        assertOperationFail(authRequest(token, 4L).get("/app/projects"));
        assertOperationFail(authRequest(token, 5L).get("/app/projects"));
    }

    @Test
    void shouldIgnoreIllegalOrgHeaderWhenMethodDeclaresNone() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("admin", "123456");

        userRequest(token).header(OrganizationScopeInterceptor.ORG_ID_HEADER, "abc").get("/app/user/")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        request().header(OrganizationScopeInterceptor.ORG_ID_HEADER, "not-an-org")
                .body(Map.of("username", "admin", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    /**
     * 切换只改默认组织，不改变另一侧请求头读到的组织。
     */
    @Test
    void shouldSwitchOnlyTheDefaultOrg() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 1L, 1L);
        String token = loginAs("admin", "123456");

        userRequest(token).body(Map.of("orgId", 2)).post("/app/user/switch")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM `user` WHERE id = 1", Long.class)).isEqualTo(2L);
        authRequest(token, 1L).get("/orgadmin/organizations")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.name", equalTo("组织1"));
        authRequest(token, 2L).get("/orgadmin/organizations")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.name", equalTo("组织2"));
        assertThat(redisTemplate.hasKey("currentOrg:user:1")).isNotEqualTo(Boolean.TRUE);
        userRequest(token).get("/app/user/")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.defaultOrgId", equalTo("2"));
    }

    /**
     * 登录返回默认组织，但该值不能授权下一次业务请求。
     */
    @Test
    void shouldReturnDefaultOrgWithoutAuthorizingTheNextRequest() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_USER");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 1L);
        String token = login(2L, "ORG_USER");

        assertOperationFail(userRequest(token).get("/app/projects"));
    }

    @Test
    void shouldKeepDisabledDefaultOrgInsteadOfChoosingAnother() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "组织2");
        createOrganizationMember(2L, 2L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE organization SET status = 0 WHERE id = 2");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = ? WHERE id = ?", 2L, 1L);

        request().body(Map.of("username", "admin", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.defaultOrgId", equalTo("2"))
                .body("data.roles", empty());
    }

    @Test
    void shouldLoginWithoutDefaultOrgWhenStoredIdIsZero() {
        createUserWithOrganization(1L, "admin", "123456", "admin@example.com", 1L, 1L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 0 WHERE id = 1");

        request().body(Map.of("username", "admin", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.defaultOrgId", nullValue())
                .body("data.roles", empty())
                .body("data.token", notNullValue());
    }

    @Test
    void shouldLoginWhenUserHasNoOrganization() {
        createTestUser(1L, "lonely", "123456", "lonely@example.com");

        request().body(Map.of("username", "lonely", "password", "123456")).post("/app/session/pwd")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.defaultOrgId", nullValue())
                .body("data.roles", empty())
                .body("data.token", notNullValue());
    }

    private String login(long expectedOrgId, String expectedRole) {
        Response response = request().body(Map.of("username", "admin", "password", "123456")).post("/app/session/pwd");
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.defaultOrgId", equalTo(Long.toString(expectedOrgId)))
                .body("data.roles", contains(expectedRole));
        return response.jsonPath().getString("data.token");
    }

    private void assertOperationFail(Response response) {
        response.then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    private Long orgIdOfProjectType(String name) {
        return jdbcTemplate.queryForObject("SELECT org_id FROM project_type WHERE name = ?", Long.class, name);
    }
}

package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class AuthorizationBoundaryApiTest extends BaseApiTest {

    /** 共享普通管理员夹具必须能通过正式角色约束和登录，同时没有 root 权限。 */
    @Test
    void shouldCreateNormalAdminUsingSharedFixture() {
        String token = createNormalAdmin();
        assertThat(jdbcTemplate.queryForObject("SELECT role FROM admin", String.class)).isEqualTo("normal");
        authRequest(token).get("/admin/organizations/").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(0));
        authRequest(token).post("/admin/organizations/invite-codes/create-org").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 普通平台管理员可查看组织，但不能执行 root 治理。 */
    @Test
    void shouldRejectNormalAdminGovernanceAndAllowRead() {
        createAdmin(1L, "normal", "123456", "normal");
        createOrganization(1L, "organization");
        String token = adminLoginAs("normal", "123456");
        authRequest(token).post("/admin/organizations/invite-codes/create-org")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).body(Map.of("status", "DISABLED")).patch("/admin/organizations/1/status")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).get("/admin/organizations/")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(1));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization WHERE id=1", Integer.class)).isEqualTo(1);
    }

    /** 组织成员禁用不影响全局登录和其他组织。 */
    @Test
    void shouldRestrictOnlyDisabledMembershipWithExistingToken() {
        createUserWithOrganization(1L, "manager", "123456", "manager@example.com", 1L, 1L, "ORG_ADMIN");
        createTestUser(2L, "member", "123456", "member@example.com");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        createOrganization(2L, "other");
        createOrganizationMember(3L, 2L, 2L, "ORG_ADMIN");
        jdbcTemplate.update("UPDATE user SET default_org_id=1 WHERE id=2");
        String memberToken = loginAs("member", "123456");
        String managerToken = loginAs("manager", "123456");
        authRequest(managerToken, 1L).body(Map.of("status", "DISABLED")).patch("/orgadmin/members/2/status")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM user WHERE id=2", Integer.class)).isEqualTo(1);
        authRequest(memberToken, 1L).body(Map.of("pageNum",1,"pageSize",10)).post("/orgadmin/members")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        authRequest(memberToken, 2L).body(Map.of("pageNum",1,"pageSize",10)).post("/orgadmin/members")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(loginAs("member", "123456")).isNotBlank();
    }

    /** 即使数据库存有非法角色，已有平台 Token 也不能获得管理员权限。 */
    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "ROOT"})
    void shouldRejectInvalidPersistedAdminRole(String role) {
        createAdmin(1L, "admin", "123456", "root");
        String token = adminLoginAs("admin", "123456");
        jdbcTemplate.update("UPDATE admin SET role=? WHERE id=1", role);

        authRequest(token).get("/admin/organizations/").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /** 组织作用域必须校验当前数据库角色，拒绝未知值和错误大小写。 */
    @ParameterizedTest
    @ValueSource(strings = {"INVALID", "org_admin"})
    void shouldRejectInvalidPersistedOrganizationRole(String role) {
        createUserWithOrganization(1L, "manager", "123456", "manager@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("manager", "123456");
        jdbcTemplate.update("UPDATE organization_member SET org_role=? WHERE id=1", role);

        authRequest(token, 1L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }
}

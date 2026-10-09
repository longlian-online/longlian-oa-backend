package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
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
        createUserWithOrganization(1L, "manager", "123456", "manager@example.com", 1L, 1L, "ORG_OWNER");
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

    /** 数据库拒绝未知角色，防止测试夹具掩盖授权错误。 */
    @Test
    void shouldRejectInvalidPersistedRoles() {
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> createAdmin(1L,"invalid","123456","INVALID"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        createOrganization(1L,"organization");
        createTestUser(2L,"member","123456","member@example.com");
        org.assertj.core.api.Assertions.assertThatThrownBy(
                () -> createOrganizationMember(1L,1L,2L,"org_admin"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}

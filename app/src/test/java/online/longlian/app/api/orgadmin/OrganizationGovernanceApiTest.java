package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.resource.ResourceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.equalTo;

class OrganizationGovernanceApiTest extends BaseApiTest {
    @Autowired private ResourceService resources;
    private String ownerToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void prepareOrganization() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_OWNER");
        for (long id = 2; id <= 4; id++) {
            createTestUser(id, "member" + id, "123456", "member" + id + "@example.com");
            createOrganizationMember(id, 1L, id, id == 4 ? "ORG_USER" : "ORG_ADMIN");
            jdbcTemplate.update("UPDATE user SET default_org_id=1 WHERE id=?", id);
        }
        ownerToken = loginAs("owner", "123456");
        adminToken = loginAs("member2", "123456");
        userToken = loginAs("member4", "123456");
    }

    /** 状态治理覆盖双方角色，包括所有者和同级管理员不可被直接操作。 */
    @ParameterizedTest
    @CsvSource({"owner,1,false", "owner,3,true", "owner,4,true", "admin,1,false", "admin,3,false", "admin,4,true",
            "user,1,false", "user,3,false", "user,4,false"})
    void shouldEnforceMemberStatusMatrix(String actor, long target, boolean allowed) {
        authRequest(token(actor), 1L).body(Map.of("status", "DISABLED"))
                .patch("/orgadmin/members/" + target + "/status")
                .then().statusCode(200).body("code", equalTo(result(allowed)));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id=?", Integer.class, target))
                .isEqualTo(allowed ? 0 : 1);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM user WHERE id=?", Integer.class, target)).isEqualTo(1);
    }

    /** 移除权限与状态治理一致，拒绝请求不得删除成员记录。 */
    @ParameterizedTest
    @CsvSource({"owner,1,false", "owner,3,true", "owner,4,true", "admin,1,false", "admin,3,false", "admin,4,true",
            "user,1,false", "user,3,false", "user,4,false"})
    void shouldEnforceMemberRemovalMatrix(String actor, long target, boolean allowed) {
        authRequest(token(actor), 1L).delete("/orgadmin/members/" + target)
                .then().statusCode(200).body("code", equalTo(result(allowed)));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE id=? AND deleted_at IS NULL", Integer.class, target))
                .isEqualTo(allowed ? 0 : 1);
    }

    /** 转让后双方旧 Token 使用新权限，创建人审计字段不随所有者改变。 */
    @Test
    void shouldTransferOwnershipAndApplyNewPermissionsToOldTokens() {
        Long creator = jdbcTemplate.queryForObject("SELECT creator_id FROM organization WHERE id=1", Long.class);
        authRequest(ownerToken, 1L).put("/orgadmin/members/2/ownership")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(role(1L)).isEqualTo("ORG_ADMIN");
        assertThat(role(2L)).isEqualTo("ORG_OWNER");
        assertThat(jdbcTemplate.queryForObject("SELECT creator_id FROM organization WHERE id=1", Long.class)).isEqualTo(creator);
        authRequest(ownerToken, 1L).put("/orgadmin/members/3/ownership")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(adminToken, 1L).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/3/role")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(role(3L)).isEqualTo("ORG_USER");
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 管理员不能提升成员；所有者升降级后原 Token 的权限立即更新。 */
    @Test
    void shouldApplyRoleChangesWithoutReloginAndRejectDirectOwnerAssignment() {
        authRequest(adminToken, 1L).body(Map.of("orgRole", "ORG_ADMIN")).patch("/orgadmin/members/4/role")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken, 1L).body(Map.of("orgRole", "ORG_ADMIN")).patch("/orgadmin/members/4/role")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(userToken, 1L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(ownerToken, 1L).body(Map.of("orgRole", "ORG_USER")).patch("/orgadmin/members/4/role")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(userToken, 1L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken, 1L).body(Map.of("orgRole", "ORG_OWNER")).patch("/orgadmin/members/4/role")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(role(4L)).isEqualTo("ORG_USER");
    }

    /** 软删除保留历史关系，重新审批创建新的普通成员，不继承原管理员权限。 */
    @Test
    void shouldRemoveAndReadmitMemberWithNewMembership() {
        authRequest(ownerToken, 1L).delete("/orgadmin/members/3")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        String removedToken = loginAs("member3", "123456");
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM user WHERE id=3", Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM organization_member WHERE id=3", Object.class)).isNotNull();
        createOrganizationUserInviteOTP("REJOIN", 1L);
        userRequest(removedToken).body(Map.of("inviteCode", "REJOIN")).post("/app/user/organizations/join-by-invite")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long application = jdbcTemplate.queryForObject("SELECT id FROM group_application", Long.class);
        authRequest(ownerToken, 1L).body(Map.of("applicationStatus", "APPROVED"))
                .put("/orgadmin/members/applications/" + application + "/review")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(ownerToken, 1L).body(Map.of("orgRole", "ORG_ADMIN")).patch("/orgadmin/members/1/role")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        Long newId = jdbcTemplate.queryForObject(
                "SELECT id FROM organization_member WHERE user_id=3 AND deleted_at IS NULL", Long.class);
        assertThat(newId).isNotEqualTo(3L);
        assertThat(role(newId)).isEqualTo("ORG_USER");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE user_id=3", Integer.class)).isEqualTo(2);
    }

    /** 被禁用成员可以退出被禁用组织，但所有者必须先转让。 */
    @Test
    void shouldAllowDisabledMemberExitAndProtectOwner() {
        userRequest(ownerToken).delete("/app/organizations/1/membership")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=4");
        jdbcTemplate.update("UPDATE organization SET status=0 WHERE id=1");
        userRequest(userToken).delete("/app/organizations/1/membership")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM user WHERE id=4", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM organization_member WHERE id=4", Object.class)).isNotNull();
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 自己、禁用成员、不可用账号及跨组织目标都不能接收所有权。 */
    @Test
    void shouldRejectInvalidOwnershipTargets() {
        authRequest(ownerToken, 1L).put("/orgadmin/members/1/ownership")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=2");
        authRequest(ownerToken, 1L).put("/orgadmin/members/2/ownership")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        jdbcTemplate.update("UPDATE user SET status=0 WHERE id=3");
        authRequest(ownerToken, 1L).put("/orgadmin/members/3/ownership")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        createOrganization(2L, "other");
        createOrganizationMember(5L, 2L, 4L, "ORG_USER");
        authRequest(ownerToken, 1L).delete("/orgadmin/members/5")
                .then().body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(ownerToken, 1L).put("/orgadmin/members/5/ownership")
                .then().body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(ownerCount()).isEqualTo(1);
        assertThat(role(1L)).isEqualTo("ORG_OWNER");
    }

    /** 解散保留历史、清理默认组织及待审申请，旧授权和资源签名失效。 */
    @Test
    void shouldDissolveAndPreserveHistoryWithoutNewResourceUrls() {
        createResource(20L, 1L, 1L);
        jdbcTemplate.update("UPDATE user SET avatar_file_id=20 WHERE id=1");
        createTestUser(10L, "pending", "123456", "pending@example.com");
        jdbcTemplate.update("UPDATE user SET status=0 WHERE id=10");
        jdbcTemplate.update("INSERT INTO group_application(id,org_id,user_id,status,application_type) VALUES(10,1,10,0,0)");
        authRequest(adminToken, 1L).delete("/orgadmin/organizations/1")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken, 1L).delete("/orgadmin/organizations/2")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(ownerToken, 1L).delete("/orgadmin/organizations/1")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM organization WHERE id=1", Object.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member", Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE default_org_id=1", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=10", Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM user WHERE id=10", Integer.class)).isZero();
        userRequest(ownerToken).get("/app/user/").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.email", equalTo("owner@example.com"))
                .body("data.avatarUrl", org.hamcrest.Matchers.nullValue());
        assertThatThrownBy(() -> resources.getResourceReadUrl(20L)).isInstanceOf(AppException.class)
                .extracting("code").isEqualTo(ResultCode.DATA_NOT_EXIT.getCode());
        authRequest(ownerToken, 1L).body(Map.of("pageNum", 1, "pageSize", 10)).post("/orgadmin/members")
                .then().body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 创建事务即建立有效所有者，不需要事后回填。 */
    @Test
    void shouldCreateOrganizationWithOwner() {
        createOrganizationCreateInviteOTP("CREATE");
        createEmailVerifyOTP("EMAIL1", 1L, "newowner@example.com");
        request().body(Map.of("inviteCode", "CREATE", "code", "EMAIL1", "username", "newowner",
                        "password", "123456", "nickname", "New", "email", "newowner@example.com", "orgName", "New Org"))
                .post("/app/user/register/create-organization")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT m.org_role FROM organization_member m JOIN user u ON u.id=m.user_id WHERE u.username='newowner'",
                String.class)).isEqualTo("ORG_OWNER");
    }

    /** 生命周期入口拒绝未认证、非法路径和不存在的成员。 */
    @Test
    void shouldRejectMissingAuthenticationInvalidPathAndMissingMembership() {
        request().delete("/app/organizations/1/membership")
                .then().body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        authRequest(ownerToken, 1L).put("/orgadmin/members/invalid/ownership")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(ownerToken, 1L).delete("/orgadmin/members/99999")
                .then().body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        userRequest(userToken).delete("/app/organizations/99999/membership")
                .then().body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /** 退出以路径组织为准，不覆盖其他组织的成员关系或默认组织。 */
    @Test
    void shouldExitPathOrganizationAndPreserveOtherMembership() {
        createOrganization(2L, "other");
        createOrganizationMember(5L, 2L, 4L, "ORG_USER");
        jdbcTemplate.update("UPDATE user SET default_org_id=2 WHERE id=4");
        authRequest(userToken, 2L).delete("/app/organizations/1/membership")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE id=4 AND deleted_at IS NULL", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE id=5 AND deleted_at IS NULL", Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM user WHERE id=4", Long.class)).isEqualTo(2L);
    }

    private String token(String actor) {
        return switch (actor) { case "owner" -> ownerToken; case "admin" -> adminToken; default -> userToken; };
    }

    private int result(boolean allowed) {
        return (allowed ? ResultCode.SUCCESS : ResultCode.UNAUTHORIZED_OPERATION).getCode();
    }

    private String role(Long memberId) {
        return jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=?", String.class, memberId);
    }

    private int ownerCount() {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_member WHERE org_id=1 AND org_role='ORG_OWNER' AND deleted_at IS NULL", Integer.class);
    }
}

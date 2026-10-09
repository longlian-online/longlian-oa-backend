package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.service.resource.ResourceService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.Map;
import static org.assertj.core.api.Assertions.*;
import static org.hamcrest.Matchers.equalTo;

class OrganizationGovernanceApiTest extends BaseApiTest {
    @Autowired private ResourceService resources;
    @Autowired
    @org.springframework.beans.factory.annotation.Qualifier("requestMappingHandlerMapping")
    private org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping requestMappings;
    private String ownerToken;
    private String adminToken;
    private String userToken;

    @BeforeEach
    void prepareOrganization() {
        createUserWithOrganization(1L,"owner","123456","owner@example.com",1L,1L,"ORG_OWNER");
        for(long id=2;id<=4;id++){
            createTestUser(id,"member"+id,"123456","member"+id+"@example.com");
            createOrganizationMember(id,1L,id,id==4?"ORG_USER":"ORG_ADMIN");
            jdbcTemplate.update("UPDATE user SET default_org_id=1 WHERE id=?",id);
        }
        ownerToken=loginAs("owner","123456");
        adminToken=loginAs("member2","123456");
        userToken=loginAs("member4","123456");
    }

    /** 状态接口完整覆盖操作者和目标角色矩阵。 */
    @ParameterizedTest
    @CsvSource({"owner,1,false","owner,3,true","owner,4,true","admin,1,false","admin,3,false","admin,4,true",
            "user,1,false","user,3,false","user,4,false"})
    void shouldEnforceMemberStatusMatrix(String actor,long target,boolean allowed){
        String token=actor.equals("owner")?ownerToken:actor.equals("admin")?adminToken:userToken;
        authRequest(token,1L).body(Map.of("status","DISABLED")).patch("/orgadmin/members/"+target+"/status")
                .then().statusCode(200).body("code",equalTo((allowed?ResultCode.SUCCESS:ResultCode.UNAUTHORIZED_OPERATION).getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id=?",Integer.class,target))
                .isEqualTo(allowed?0:1);
    }

    /** 所有者转让后，双方原 Token 下一次请求使用新的权限。 */
    @Test void shouldTransferOwnershipAndApplyNewPermissionsToOldTokens(){
        authRequest(ownerToken,1L).put("/orgadmin/members/2/ownership").then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=1",String.class)).isEqualTo("ORG_ADMIN");
        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=2",String.class)).isEqualTo("ORG_OWNER");
        authRequest(ownerToken,1L).put("/orgadmin/members/3/ownership").then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(adminToken,1L).body(Map.of("orgRole","ORG_USER")).patch("/orgadmin/members/3/role")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 提升与降级不需要重新登录，直接变更所有者角色的请求被 DTO 拒绝。 */
    @Test void shouldApplyRoleChangesWithoutReloginAndRejectDirectOwnerAssignment(){
        authRequest(ownerToken,1L).body(Map.of("orgRole","ORG_ADMIN")).patch("/orgadmin/members/4/role")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(userToken,1L).body(Map.of("pageNum",1,"pageSize",10)).post("/orgadmin/members")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(ownerToken,1L).body(Map.of("orgRole","ORG_USER")).patch("/orgadmin/members/4/role")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(userToken,1L).body(Map.of("pageNum",1,"pageSize",10)).post("/orgadmin/members")
                .then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken,1L).body(Map.of("orgRole","ORG_OWNER")).patch("/orgadmin/members/4/role")
                .then().body("code",equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 移除保留历史成员且允许重新审批入组，不继承管理员角色。 */
    @Test void shouldRemoveAndReadmitMemberWithNewMembership(){
        authRequest(adminToken,1L).delete("/orgadmin/members/3")
                .then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken,1L).delete("/orgadmin/members/4")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM user WHERE id=4",Long.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM organization_member WHERE id=4",Object.class)).isNotNull();
        createOrganizationUserInviteOTP("REJOIN",1L);
        userRequest(userToken).body(Map.of("inviteCode","REJOIN")).post("/app/user/organizations/join-by-invite")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        Long application=jdbcTemplate.queryForObject("SELECT id FROM group_application",Long.class);
        authRequest(ownerToken,1L).body(Map.of("applicationStatus","APPROVED")).put("/orgadmin/members/applications/"+application+"/review")
                .then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        Long newId=jdbcTemplate.queryForObject("SELECT id FROM organization_member WHERE user_id=4 AND deleted_at IS NULL",Long.class);
        assertThat(newId).isNotEqualTo(4L);
        assertThat(jdbcTemplate.queryForObject("SELECT org_role FROM organization_member WHERE id=?",String.class,newId)).isEqualTo("ORG_USER");
    }

    /** 被禁用成员可以退出，但所有者必须先转让。 */
    @Test void shouldAllowDisabledMemberExitAndProtectOwner(){
        userRequest(ownerToken).delete("/app/organizations/1/membership").then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=4");
        jdbcTemplate.update("UPDATE organization SET status=0 WHERE id=1");
        userRequest(userToken).delete("/app/organizations/1/membership").then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM user WHERE id=4",Integer.class)).isEqualTo(1);
    }

    /** 跨组织目标、禁用目标及转让给自己都被拒绝。 */
    @Test void shouldRejectInvalidOwnershipTargets(){
        authRequest(ownerToken,1L).put("/orgadmin/members/1/ownership").then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
        jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=2");
        authRequest(ownerToken,1L).put("/orgadmin/members/2/ownership").then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
        createOrganization(2L,"other");
        createOrganizationMember(5L,2L,4L,"ORG_USER");
        authRequest(ownerToken,1L).delete("/orgadmin/members/5").then().body("code",equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(ownerToken,1L).put("/orgadmin/members/5/ownership").then().body("code",equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(ownerCount()).isEqualTo(1);
    }

    /** 解散阻断旧授权和文件签名，同时保留成员、资源历史。 */
    @Test void shouldDissolveAndPreserveHistoryWithoutNewResourceUrls(){
        createResource(20L,1L,1L);
        jdbcTemplate.update("UPDATE resource SET process_status=1 WHERE id=20");
        jdbcTemplate.update("INSERT INTO group_application(id,org_id,status,application_type,username,email,password_hash) VALUES(10,1,0,0,'pending','pending@example.com',?)",passwordEncoder.encode("123456"));
        authRequest(adminToken,1L).delete("/orgadmin/organizations/1").then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(ownerToken,1L).delete("/orgadmin/organizations/2").then().body("code",equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(ownerToken,1L).delete("/orgadmin/organizations/1").then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT deleted_at FROM organization WHERE id=1",Object.class)).isNotNull();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member",Integer.class)).isEqualTo(4);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM resource",Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM group_application WHERE id=10",String.class)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=10",Integer.class)).isEqualTo(2);
        assertThatThrownBy(()->resources.getResourceReadUrl(20L)).isInstanceOf(AppException.class);
        authRequest(ownerToken,1L).body(Map.of("pageNum",1,"pageSize",10)).post("/orgadmin/members")
                .then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 历史异常数据没有数据库 CHECK 时，应用仍拒绝继续治理。 */
    @ParameterizedTest
    @ValueSource(strings = {"missing", "duplicate", "disabled", "disabled-duplicate", "invalid-role"})
    void shouldRejectGovernanceWithInvalidOwnerState(String state){
        switch (state) {
            case "missing" -> jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_ADMIN' WHERE id=1");
            case "duplicate" -> jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_OWNER' WHERE id=2");
            case "disabled" -> jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=1");
            case "disabled-duplicate" -> {
                jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_OWNER' WHERE id=2");
                jdbcTemplate.update("UPDATE organization_member SET status=0 WHERE id=1");
            }
            case "invalid-role" -> jdbcTemplate.update("UPDATE organization_member SET org_role='org_owner' WHERE id=1");
            default -> throw new IllegalArgumentException(state);
        }
        authRequest(loginAs("member3","123456"),1L).body(Map.of("status","DISABLED"))
                .patch("/orgadmin/members/4/status").then().statusCode(200)
                .body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM organization_member WHERE id=4",Integer.class)).isEqualTo(1);
    }

    /** 新组织在创建事务中即拥有有效所有者。 */
    @Test void shouldCreateOrganizationWithOwner(){
        createOrganizationCreateInviteOTP("CREATE");
        createEmailVerifyOTP("EMAIL1",1L,"newowner@example.com");
        request().body(Map.of("inviteCode","CREATE","code","EMAIL1","username","newowner",
                "password","123456","nickname","New","email","newowner@example.com","orgName","New Org"))
                .post("/app/user/register/create-organization").then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT m.org_role FROM organization_member m JOIN user u ON u.id=m.user_id WHERE u.username='newowner'",String.class))
                .isEqualTo("ORG_OWNER");
    }


    /** 新生命周期接口验证认证、路径类型及缺失成员边界。 */
    @Test void shouldRejectMissingAuthenticationInvalidPathAndOwnerRemoval(){
        request().delete("/app/organizations/1/membership").then().body("code",equalTo(ResultCode.UNAUTHORIZED.getCode()));
        authRequest(ownerToken,1L).put("/orgadmin/members/invalid/ownership").then().body("code",equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(ownerToken,1L).delete("/orgadmin/members/99999").then().body("code",equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        authRequest(ownerToken,1L).delete("/orgadmin/members/1").then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(adminToken,1L).delete("/orgadmin/members/2").then().body("code",equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    /** 移除接口也必须完整校验双方角色。 */
    @ParameterizedTest
    @CsvSource({"owner,1,false","owner,3,true","owner,4,true","admin,1,false","admin,3,false","admin,4,true",
            "user,1,false","user,3,false","user,4,false"})
    void shouldEnforceMemberRemovalMatrix(String actor,long target,boolean allowed){
        String token=actor.equals("owner")?ownerToken:actor.equals("admin")?adminToken:userToken;
        authRequest(token,1L).delete("/orgadmin/members/"+target).then()
                .body("code",equalTo((allowed?ResultCode.SUCCESS:ResultCode.UNAUTHORIZED_OPERATION).getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE id=? AND deleted_at IS NULL",Integer.class,target))
                .isEqualTo(allowed?0:1);
    }

    /** 所有者回填未完成时，普通管理员不能继续执行治理写入。 */
    @Test void shouldRejectGovernanceWithoutOwner(){
        jdbcTemplate.update("UPDATE organization_member SET org_role='ORG_ADMIN' WHERE id=1");
        authRequest(adminToken,1L).body(Map.of("status","DISABLED")).patch("/orgadmin/members/4/status")
                .then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 移除某组织关系不覆盖用户保存的另一默认组织。 */
    @Test void shouldPreserveAnotherDefaultOrganization(){
        createOrganization(2L,"other");
        createOrganizationMember(5L,2L,4L,"ORG_USER");
        jdbcTemplate.update("UPDATE user SET default_org_id=2 WHERE id=4");
        authRequest(ownerToken,1L).delete("/orgadmin/members/4").then().body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT default_org_id FROM user WHERE id=4",Long.class)).isEqualTo(2L);
        jdbcTemplate.update("UPDATE user SET status=0 WHERE id=3");
        authRequest(ownerToken,1L).put("/orgadmin/members/3/ownership").then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 实际注册的生命周期路由必须由对应管理端或用户端包中的控制器提供。 */
    @Test
    void shouldRegisterLifecycleRoutesInMatchingControllerPackages() {
        Map<String, String> expectedPackages = Map.of(
                "/app/organizations/{orgId}/membership", "online.longlian.app.controller.app",
                "/orgadmin/members/{memberId}", "online.longlian.app.controller.orgadmin",
                "/orgadmin/members/{memberId}/ownership", "online.longlian.app.controller.orgadmin",
                "/orgadmin/organizations/{orgId}", "online.longlian.app.controller.orgadmin");
        expectedPackages.forEach((route, expectedPackage) -> {
            var handlers = requestMappings.getHandlerMethods().entrySet().stream()
                    .filter(entry -> entry.getKey().getPatternValues().contains(route))
                    .map(entry -> entry.getValue().getBeanType().getPackageName())
                    .toList();
            assertThat(handlers).containsExactly(expectedPackage);
        });
    }

    /** 用户退出以路径组织为准，其他组织请求头不能改变退出对象。 */
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

    private int ownerCount(){return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member WHERE org_id=1 AND org_role='ORG_OWNER' AND deleted_at IS NULL",Integer.class);}
}

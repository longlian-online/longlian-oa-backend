package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class RegistrationApplicationApiTest extends BaseApiTest {
    /** 拒绝注册快照后身份可以再次提交，正式用户直到批准才创建。 */
    @Test
    void shouldReuseIdentityAfterRejectionAndCreateUserOnApproval() {
        createUserWithOrganization(1L,"manager","123456","manager@example.com",1L,1L,"ORG_ADMIN");
        String token = loginAs("manager","123456");
        submit("JOIN01","EMAIL1");
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username='newuser'",Integer.class)).isZero();
        Long first = jdbcTemplate.queryForObject("SELECT id FROM group_application",Long.class);
        authRequest(token,1L).body(Map.of("applicationStatus","REJECTED")).put("/orgadmin/members/applications/"+first+"/review")
                .then().statusCode(200).body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM group_application WHERE id=?",String.class,first)).isNull();
        submit("JOIN02","EMAIL2");
        Long second = jdbcTemplate.queryForObject("SELECT id FROM group_application WHERE status=0",Long.class);
        authRequest(token,1L).body(Map.of("applicationStatus","APPROVED")).put("/orgadmin/members/applications/"+second+"/review")
                .then().statusCode(200).body("code",equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user WHERE username='newuser' AND status=1",Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject("SELECT password_hash FROM group_application WHERE id=?",String.class,second)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_join_otp o " +
                        "JOIN group_application a ON a.otp_id = o.otp_id " +
                        "JOIN organization_member m ON m.id = o.org_member_id " +
                        "WHERE a.id = ? AND o.invited_user_id = a.user_id " +
                        "AND m.user_id = a.user_id AND m.org_id = a.org_id",
                Integer.class, second)).isEqualTo(1);
        assertThat(loginAs("newuser","123456")).isNotBlank();
        authRequest(token,1L).body(Map.of("applicationStatus","APPROVED")).put("/orgadmin/members/applications/"+second+"/review")
                .then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    /** 正式身份在审批前被占用时完整回滚，申请仍待审。 */
    @Test
    void shouldKeepPendingWhenIdentityIsTakenBeforeApproval() {
        createUserWithOrganization(1L,"manager","123456","manager@example.com",1L,1L,"ORG_ADMIN");
        submit("JOIN01","EMAIL1");
        createTestUser(2L,"newuser","123456","other@example.com");
        Long application = jdbcTemplate.queryForObject("SELECT id FROM group_application",Long.class);
        authRequest(loginAs("manager","123456"),1L).body(Map.of("applicationStatus","APPROVED"))
                .put("/orgadmin/members/applications/"+application+"/review")
                .then().body("code",equalTo(ResultCode.OPERATION_FAIL.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM group_application WHERE id=?",Integer.class,application)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization_member",Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT password_hash FROM group_application WHERE id = ?", String.class, application)).isNotNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT reviewer_id FROM group_application WHERE id = ?", Long.class, application)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM `user` WHERE email = ?", Integer.class, "new@example.com")).isZero();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_join_otp WHERE org_member_id IS NOT NULL", Integer.class)).isZero();
    }

    /** 禁用组织不能展示有效邀请或接受注册申请，失败不得消耗验证码。 */
    @Test
    void shouldRejectDisabledOrganizationWithoutConsumingCodes() {
        createOrganization(1L, "disabled");
        createOrganizationUserInviteOTP("JOIN01", 1L);
        createEmailVerifyOTP("EMAIL1", 1L, "new@example.com");
        jdbcTemplate.update("UPDATE organization SET status = 0 WHERE id = ?", 1L);

        request().queryParam("inviteCode", "JOIN01")
                .get("/app/user/register/join-organization/invite-info")
                .then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        request().body(Map.of("username", "newuser", "password", "123456", "nickname", "New",
                "email", "new@example.com", "inviteCode", "JOIN01", "code", "EMAIL1"))
                .post("/app/user/register/join-organization")
                .then().statusCode(200).body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));

        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM group_application", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code = ?",
                Integer.class, "JOIN01")).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT status FROM one_time_password WHERE code = ?",
                Integer.class, "EMAIL1")).isZero();
    }

    private void submit(String invite, String code) {
        createOrganizationUserInviteOTP(invite,1L);
        createEmailVerifyOTP(code,1L,"new@example.com");
        request().body(Map.of("username","newuser","password","123456","nickname","New",
                "email","new@example.com","inviteCode",invite,"code",code))
                .post("/app/user/register/join-organization")
                .then().statusCode(200).body("code",equalTo(ResultCode.SUCCESS.getCode()));
    }
}

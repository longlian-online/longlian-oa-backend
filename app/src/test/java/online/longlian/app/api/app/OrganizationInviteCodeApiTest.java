package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class OrganizationInviteCodeApiTest extends BaseApiTest {

    @Test
    void shouldStoreChosenExpiryForCreateAndJoinInvites() {
        createAdmin(1L, "root", "123456", "root");
        String adminToken = adminLoginAs("root", "123456");
        createUserWithOrganization(2L, "orgadmin", "123456", "orgadmin@example.com", 2L, 8L, "ORG_ADMIN");
        String orgToken = loginAs("orgadmin", "123456");

        authRequest(adminToken)
                .body(Map.of("expiredAt", "2099-03-01 08:30:00"))
                .post("/admin/organizations/invite-codes/create-org")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.inviteCode", notNullValue())
                .body("data.expireAt", equalTo("2099-03-01 08:30:00"));

        authRequest(orgToken)
                .body(Map.of("maxUses", 5))
                .post("/orgadmin/members/invite-codes/join-org")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.inviteCode", notNullValue())
                .body("data.maxUses", equalTo(5))
                .body("data.expireAt", equalTo(null));
    }

    @Test
    void shouldRejectMissingOrPastExpiry() {
        createAdmin(1L, "root", "123456", "root");
        String token = adminLoginAs("root", "123456");

        authRequest(token)
                .body(Map.of())
                .post("/admin/organizations/invite-codes/create-org")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        authRequest(token)
                .body(Map.of("expiredAt", "2000-01-01 00:00:00"))
                .post("/admin/organizations/invite-codes/create-org")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectInvalidExpiredAndAcceptedInviteCodes() {
        createOrganization(8L, "目标组织");
        createOrganizationInviteCode("GONE01", 2, 8L, LocalDateTime.of(2000, 1, 1, 0, 0), 0, null);
        createOrganizationInviteCode("USED01", 2, 8L, testNow().plusDays(1), 1, testNow().minusMinutes(5));

        request().get("/app/user/register/join-organization/invite-info?inviteCode=NOPE01")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        request().get("/app/user/register/join-organization/invite-info?inviteCode=GONE01")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        request().get("/app/user/register/join-organization/invite-info?inviteCode=USED01")
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    @Test
    void shouldAllowUnlimitedInviteCodeToBeQueriedRepeatedly() {
        createUserWithOrganization(2L, "orgadmin", "123456", "orgadmin@example.com", 2L, 8L, "ORG_ADMIN");
        String orgToken = loginAs("orgadmin", "123456");
        String inviteCode = authRequest(orgToken)
                .body(Map.of())
                .post("/orgadmin/members/invite-codes/join-org")
                .then()
                .statusCode(200)
                .extract()
                .path("data.inviteCode");

        request().get("/app/user/register/join-organization/invite-info?inviteCode=" + inviteCode)
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        request().get("/app/user/register/join-organization/invite-info?inviteCode=" + inviteCode)
                .then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.orgName", equalTo("组织8"));
    }
}

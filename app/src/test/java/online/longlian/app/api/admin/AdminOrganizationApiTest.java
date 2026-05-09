package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class AdminOrganizationApiTest extends BaseApiTest {

    @Test
    void shouldListOrganizationsSuccessfully() {
        createAdmin(1L, "superadmin", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin", "123456");

        createOrganization(1L, "测试组织1");
        createOrganization(2L, "测试组织2");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/admin/organizations/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", greaterThanOrEqualTo(2));
    }

    @Test
    void shouldListOrganizationsWithNameFilter() {
        createAdmin(2L, "superadmin2", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin2", "123456");

        createOrganization(3L, "筛选测试组织");
        createOrganization(4L, "其他组织");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .queryParam("orgName", "筛选")
                .get("/admin/organizations/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue());
    }

    @Test
    void shouldGenerateCreateOrgInviteCode() {
        createAdmin(3L, "superadmin3", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin3", "123456");

        Response response = authRequest(token)
                .post("/admin/organizations/invite-codes/create-org");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("生成成功"))
                .body("data.inviteCode", notNullValue())
                .body("data.expireAt", notNullValue());
    }

    @Test
    void shouldFailGenerateInviteCodeWithoutAuth() {
        Response response = request()
                .post("/admin/organizations/invite-codes/create-org");

        response
                .then()
                .statusCode(401);
    }

    @Test
    void shouldChangeOrgStatusToDisabled() {
        createAdmin(4L, "superadmin4", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin4", "123456");

        createOrganization(5L, "状态测试组织");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/admin/organizations/5/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("ok"));
    }

    @Test
    void shouldChangeOrgStatusToEnabled() {
        createAdmin(5L, "superadmin5", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin5", "123456");

        createOrganization(6L, "状态测试组织2");

        authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/admin/organizations/6/status");

        Response response = authRequest(token)
                .body(Map.of("status", "ENABLED"))
                .patch("/admin/organizations/6/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("ok"));
    }

    @Test
    void shouldFailChangeOrgStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "DISABLED"))
                .patch("/admin/organizations/999/status");

        response
                .then()
                .statusCode(401);
    }

    @Test
    void shouldFailChangeNonexistentOrgStatus() {
        createAdmin(6L, "superadmin6", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin6", "123456");

        Response response = authRequest(token)
                .body(Map.of("status", "DISABLED"))
                .patch("/admin/organizations/99999/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldFailChangeOrgStatusWithInvalidStatus() {
        createAdmin(7L, "superadmin7", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin7", "123456");

        createOrganization(7L, "测试组织7");

        Response response = authRequest(token)
                .body(Map.of("status", "INVALID_STATUS"))
                .patch("/admin/organizations/7/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailChangeOrgStatusWithNullStatus() {
        createAdmin(8L, "superadmin8", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin8", "123456");

        createOrganization(8L, "测试组织8");

        Response response = authRequest(token)
                .body(Map.of("status", null))
                .patch("/admin/organizations/8/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldListOrganizationsWithZeroPageNum() {
        createAdmin(9L, "superadmin9", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin9", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 0)
                .queryParam("pageSize", 10)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldListOrganizationsWithNegativePageNum() {
        createAdmin(10L, "superadmin10", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin10", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", -1)
                .queryParam("pageSize", 10)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldListOrganizationsWithZeroPageSize() {
        createAdmin(11L, "superadmin11", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin11", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 0)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldListOrganizationsWithNegativePageSize() {
        createAdmin(12L, "superadmin12", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin12", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", -1)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldListOrganizationsWithDefaultPagination() {
        createAdmin(13L, "superadmin13", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin13", "123456");

        Response response = authRequest(token)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0)).body("data.list", notNullValue());
    }

    @Test
    void shouldListOrganizationsWithLongOrgName() {
        createAdmin(14L, "superadmin14", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin14", "123456");

        Response response = authRequest(token)
                .queryParam("orgName", "a".repeat(50))
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldListOrganizationsWithSqlInjectionAttempt() {
        createAdmin(15L, "superadmin15", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin15", "123456");

        Response response = authRequest(token)
                .queryParam("orgName", "'\"; DROP TABLE--")
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));
    }

    @Test
    void shouldFailGenerateInviteCodeWithNormalAdminRole() {
        createAdmin(16L, "superadmin16", "123456", "root");
        createAdmin(17L, "normaladmin", "123456", "ADMIN");
        String normalToken = adminLoginAs("normaladmin", "123456");

        Response response = authRequest(normalToken)
                .post("/admin/organizations/invite-codes/create-org");

        response.then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }
}

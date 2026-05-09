package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
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
}

package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class AdminOrganizationApiTest extends BaseApiTest {

    /**
     * 分页查询组织列表成功
     */
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

    /**
     * 按组织名称筛选查询组织列表成功
     */
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

    /**
     * 生成创建组织邀请码成功
     */
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

    /**
     * 无认证生成邀请码失败
     */
    @Test
    void shouldFailGenerateInviteCodeWithoutAuth() {
        Response response = request()
                .post("/admin/organizations/invite-codes/create-org");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 禁用组织成功
     */
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

    /**
     * 启用组织成功
     */
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

    /**
     * 无认证修改组织状态失败
     */
    @Test
    void shouldFailChangeOrgStatusWithoutAuth() {
        Response response = request()
                .body(Map.of("status", "DISABLED"))
                .patch("/admin/organizations/999/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 修改不存在的组织状态失败
     */
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

    /**
     * 使用无效状态修改组织状态失败
     */
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

    /**
     * 使用空状态修改组织状态失败
     */
    @Test
    void shouldFailChangeOrgStatusWithNullStatus() {
        createAdmin(8L, "superadmin8", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin8", "123456");

        createOrganization(8L, "测试组织8");

        java.util.Map<String, String> body = new java.util.HashMap<>();
        body.put("status", null);

        Response response = authRequest(token)
                .body(body)
                .patch("/admin/organizations/8/status");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * pageNum 为 0 时查询组织列表失败
     */
    @Test
    void shouldFailWithInvalidPageNum() {
        createAdmin(18L, "superadmin18", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin18", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 0)
                .queryParam("pageSize", 10)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * pageNum 为负数时查询组织列表失败
     */
    @Test
    void shouldFailWithNegativePageNum() {
        createAdmin(19L, "superadmin19", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin19", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", -1)
                .queryParam("pageSize", 10)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * pageSize 为 0 时查询组织列表失败
     */
    @Test
    void shouldFailWithZeroPageSize() {
        createAdmin(20L, "superadmin20", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin20", "123456");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 0)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 组织名称过长时查询组织列表失败
     */
    @Test
    void shouldFailWithTooLongOrgName() {
        createAdmin(22L, "superadmin22", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin22", "123456");

        Response response = authRequest(token)
                .queryParam("orgName", "a".repeat(50))
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /**
     * 使用 SQL 注入尝试查询组织列表（防注入验证）
     */
    @Test
    void shouldListOrganizationsWithSqlInjectionAttempt() {
        createAdmin(15L, "superadmin15", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin15", "123456");

        createOrganization(15L, "正常组织");

        Response response = authRequest(token)
                .queryParam("orgName", "'\"; DROP TABLE--")
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0));

        jdbcTemplate.queryForObject("SELECT COUNT(*) FROM organization WHERE id = 15", Integer.class);
    }

    /**
     * 不带分页参数时使用默认分页
     */
    @Test
    void shouldListOrganizationsWithDefaultPagination() {
        createAdmin(23L, "superadmin23", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin23", "123456");

        Response response = authRequest(token)
                .get("/admin/organizations/");

        response.then().statusCode(200).body("code", equalTo(0)).body("data.list", notNullValue());
    }
}

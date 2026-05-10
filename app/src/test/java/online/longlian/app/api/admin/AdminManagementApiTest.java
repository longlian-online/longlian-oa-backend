package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.hamcrest.Matchers.*;

public class AdminManagementApiTest extends BaseApiTest {

    @Test
    void shouldCreateAdminSuccessfully() {
        createAdmin(1L, "superadmin", "123456", "root");
        String token = adminLoginAs("superadmin", "123456");

        Response response = authRequest(token)
                .body(Map.of("username", "newadmin", "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("创建成功"))
                .body("data", notNullValue());
    }

    @Test
    void shouldFailCreateAdminWithoutAuth() {
        Response response = request()
                .body(Map.of("username", "newadmin", "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(401);
    }

    @Test
    void shouldDeleteAdminSuccessfully() {
        createAdmin(2L, "superadmin2", "123456", "root");
        String token = adminLoginAs("superadmin2", "123456");

        createAdmin(3L, "todelete", "123456", "ADMIN");

        Response response = authRequest(token)
                .delete("/admin/admins/3");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("msg", equalTo("删除成功"));
    }

    @Test
    void shouldListAdminsSuccessfully() {
        createAdmin(4L, "superadmin3", "123456", "root");
        String token = adminLoginAs("superadmin3", "123456");

        createAdmin(5L, "admin5", "123456", "ADMIN");
        createAdmin(6L, "admin6", "123456", "ADMIN");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list", notNullValue())
                .body("data.total", greaterThanOrEqualTo(3));
    }

    @Test
    void shouldListAdminsWithPagination() {
        createAdmin(7L, "superadmin4", "123456", "root");
        String token = adminLoginAs("superadmin4", "123456");

        createAdmin(8L, "admin8", "123456", "ADMIN");
        createAdmin(9L, "admin9", "123456", "ADMIN");
        createAdmin(10L, "admin10", "123456", "ADMIN");

        Response response = authRequest(token)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 2)
                .get("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data.list.size()", equalTo(2))
                .body("data.total", greaterThanOrEqualTo(4));
    }

    @Test
    void shouldFailCreateAdminWithNormalAdminRole() {
        createAdmin(11L, "superadmin5", "123456", "root");
        createAdmin(12L, "normaladmin", "123456", "ADMIN");
        String normalToken = adminLoginAs("normaladmin", "123456");

        Response response = authRequest(normalToken)
                .body(Map.of("username", "newadmin", "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailDeleteAdminWithNormalAdminRole() {
        createAdmin(13L, "superadmin6", "123456", "root");
        createAdmin(14L, "normaladmin2", "123456", "ADMIN");
        String normalToken = adminLoginAs("normaladmin2", "123456");

        createAdmin(15L, "todelete", "123456", "ADMIN");

        Response response = authRequest(normalToken)
                .delete("/admin/admins/15");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailDeleteRootAdmin() {
        createAdmin(16L, "superadmin7", "123456", "root");
        String token = adminLoginAs("superadmin7", "123456");

        Response response = authRequest(token)
                .delete("/admin/admins/16");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailDeleteSelf() {
        createAdmin(17L, "superadmin8", "123456", "root");
        String token = adminLoginAs("superadmin8", "123456");

        Response response = authRequest(token)
                .delete("/admin/admins/17");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
    }

    @Test
    void shouldFailDeleteNonexistentAdmin() {
        createAdmin(18L, "superadmin9", "123456", "root");
        String token = adminLoginAs("superadmin9", "123456");

        Response response = authRequest(token)
                .delete("/admin/admins/99999");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithDuplicateUsername() {
        createAdmin(19L, "superadmin10", "123456", "root");
        String token = adminLoginAs("superadmin10", "123456");

        createAdmin(20L, "existingadmin", "123456", "ADMIN");

        Response response = authRequest(token)
                .body(Map.of("username", "existingadmin", "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithShortUsername() {
        createAdmin(21L, "superadmin11", "123456", "root");
        String token = adminLoginAs("superadmin11", "123456");

        Response response = authRequest(token)
                .body(Map.of("username", "ab", "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithLongUsername() {
        createAdmin(22L, "superadmin12", "123456", "root");
        String token = adminLoginAs("superadmin12", "123456");

        Response response = authRequest(token)
                .body(Map.of("username", "a".repeat(33), "password", "654321"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithShortPassword() {
        createAdmin(23L, "superadmin13", "123456", "root");
        String token = adminLoginAs("superadmin13", "123456");

        Response response = authRequest(token)
                .body(Map.of("username", "newadmin", "password", "12345"))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithLongPassword() {
        createAdmin(24L, "superadmin14", "123456", "root");
        String token = adminLoginAs("superadmin14", "123456");

        Response response = authRequest(token)
                .body(Map.of("username", "newadmin", "password", "a".repeat(65)))
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldFailCreateAdminWithEmptyBody() {
        createAdmin(25L, "superadmin15", "123456", "root");
        String token = adminLoginAs("superadmin15", "123456");

        Response response = authRequest(token)
                .body("{}")
                .post("/admin/admins/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }
}

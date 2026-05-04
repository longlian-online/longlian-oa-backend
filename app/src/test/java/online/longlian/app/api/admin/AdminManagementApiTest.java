package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
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
}

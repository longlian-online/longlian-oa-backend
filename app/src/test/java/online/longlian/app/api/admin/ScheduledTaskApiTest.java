package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

public class ScheduledTaskApiTest extends BaseApiTest {

    @Test
    void shouldListAllScheduledTasks() {
        createAdmin(1L, "superadmin", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin", "123456");

        Response response = authRequest(token)
                .get("/admin/scheduled-tasks/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(0))
                .body("data", notNullValue())
                .body("data.size()", greaterThanOrEqualTo(0));
    }

    @Test
    void shouldFailListTasksWithoutAuth() {
        Response response = request()
                .get("/admin/scheduled-tasks/");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    @Test
    void shouldTriggerScheduledTask() {
        createAdmin(2L, "superadmin2", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin2", "123456");

        Response listResponse = authRequest(token)
                .get("/admin/scheduled-tasks/");

        listResponse
                .then()
                .statusCode(200);

        if (listResponse.jsonPath().getList("data").size() > 0) {
            String taskName = listResponse.jsonPath().getString("data[0].taskName");

            Response triggerResponse = authRequest(token)
                    .body("{}")
                    .post("/admin/scheduled-tasks/" + taskName + "/trigger");

            triggerResponse
                    .then()
                    .statusCode(200)
                    .body("code", equalTo(0))
                    .body("msg", equalTo("触发成功"));
        }
    }

    @Test
    void shouldFailTriggerNonexistentTask() {
        createAdmin(3L, "superadmin3", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin3", "123456");

        Response response = authRequest(token)
                .body("{}")
                .post("/admin/scheduled-tasks/nonexistent-task/trigger");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }

    @Test
    void shouldFailTriggerTaskWithoutAuth() {
        Response response = request()
                .body("{}")
                .post("/admin/scheduled-tasks/heartbeat/trigger");

        response
                .then()
                .statusCode(200)
                .body("code", not(equalTo(0)));
    }
}

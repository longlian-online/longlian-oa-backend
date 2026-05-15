package online.longlian.app.api.admin;

import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

public class ScheduledTaskApiTest extends BaseApiTest {

    /**
     * 分页查询所有定时任务成功
     */
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

    /**
     * 无认证查询定时任务列表失败
     */
    @Test
    void shouldFailListTasksWithoutAuth() {
        Response response = request()
                .get("/admin/scheduled-tasks/");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /**
     * 手动触发定时任务成功
     */
    @Test
    void shouldTriggerScheduledTask() {
        createAdmin(2L, "superadmin2", "123456", "SUPER_ADMIN");
        String token = adminLoginAs("superadmin2", "123456");

        Response listResponse = authRequest(token)
                .get("/admin/scheduled-tasks/");

        listResponse
                .then()
                .statusCode(200);

        if (!listResponse.jsonPath().getList("data").isEmpty()) {
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

    /**
     * 触发不存在的定时任务失败
     */
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
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    /**
     * 无认证触发定时任务失败
     */
    @Test
    void shouldFailTriggerTaskWithoutAuth() {
        Response response = request()
                .body("{}")
                .post("/admin/scheduled-tasks/heartbeat/trigger");

        response
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }
}

package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.hasKey;

public class BaseTaskOptionApiTest extends BaseApiTest {

    @Test
    void shouldListEnabledTasksForOrdinaryMember() {
        createUserWithOrganization(1L, "member", "123456", "member@example.com", 1L, 1L, "ORG_USER");
        createOrganization(2L, "其他组织");
        jdbcTemplate.update(
                "INSERT INTO base_task (id, org_id, name, icon, description, meta_schema, status, creator_id, deleted_at) VALUES "
                        + "(1, 1, 'translate', 'Languages', '说明', '[{\"key\":\"secret\"}]', 1, 1, NULL), "
                        + "(2, 1, 'review', NULL, NULL, '[]', 1, 1, NULL), "
                        + "(3, 1, 'disabled', NULL, NULL, '[]', 0, 1, NULL), "
                        + "(4, 1, 'deleted', NULL, NULL, '[]', 1, 1, NOW()), "
                        + "(5, 2, 'foreign', NULL, NULL, '[]', 1, 1, NULL)");

        authRequest(loginAs("member", "123456")).get("/app/task/base/options").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(2))
                .body("data[0].id", equalTo("2"))
                .body("data[0].name", equalTo("review"))
                .body("data[0].status", equalTo("ENABLED"))
                .body("data[0]", not(hasKey("submitFields")))
                .body("data[1].id", equalTo("1"))
                .body("data[1].name", equalTo("translate"))
                .body("data[1].icon", equalTo("Languages"))
                .body("data[1].description", equalTo("说明"))
                .body("data[1]", not(hasKey("metaSchema")));
    }

    @Test
    void shouldReturnEmptyWhenOrganizationHasNoEnabledTasks() {
        createUserWithOrganization(3L, "empty", "123456", "empty@example.com", 3L, 3L, "ORG_USER");

        authRequest(loginAs("empty", "123456")).get("/app/task/base/options").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", hasSize(0));
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        request().get("/app/task/base/options").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldRejectOrganizationTheMemberDoesNotBelongTo() {
        createUserWithOrganization(1L, "member", "123456", "member@example.com", 1L, 1L, "ORG_USER");
        createOrganization(2L, "其他组织");

        authRequest(loginAs("member", "123456"), 2L).get("/app/task/base/options").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }
}

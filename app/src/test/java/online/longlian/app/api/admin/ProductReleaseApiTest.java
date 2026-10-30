package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

class ProductReleaseApiTest extends BaseApiTest {

    @Test
    void shouldPublishFirstReleaseAndHideComponentVersionsFromUsers() {
        String admin = createNormalAdmin();
        createTestUser(11L, "reader", "123456", "reader@example.com");
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);

        String body = userRequest(loginAs("reader", "123456"))
                .get("/app/product-releases/current")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.release.version", equalTo("1.0.0"))
                .body("data.release.title", equalTo("标题1.0.0"))
                .body("data.release.added[0]", equalTo("新增1.0.0"))
                .body("data.olderCount", equalTo(0))
                .body("data.release.frontendVersion", nullValue())
                .extract()
                .asString();
        assertThat(body).doesNotContain("frontendVersion").doesNotContain("backendVersion");
    }

    @Test
    void shouldPublishWhenOnlyOneSideChangesAndRejectAnUnchangedPair() {
        String admin = createNormalAdmin();
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);

        authRequest(admin)
                .body(body("1.2.0", "0.0.18", "1.0.52", true))
                .post("/admin/product-releases/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,这次没有更换前端或后端版本"));

        publish(admin, "1.3.0", "0.0.19", "1.0.52", true);
        assertThat(statusOf("1.3.0")).isEqualTo(1);
        assertThat(column("1.3.0", "backend_version")).isEqualTo("1.0.52");
    }

    @Test
    void shouldAllowDraftToReusePairAndRejectBindingChangeAfterPublish() {
        String admin = createNormalAdmin();
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);
        publish(admin, "1.1.0", "0.0.18", "1.0.52", false);
        assertThat(statusOf("1.1.0")).isZero();

        LocalDateTime publishedAt = publishedAt("1.0.0");
        authRequest(admin)
                .body(Map.of(
                        "version", "1.0.1",
                        "frontendVersion", "0.0.20",
                        "backendVersion", "1.0.52",
                        "title", "改过的标题",
                        "summary", "改过的摘要",
                        "added", List.of("仍然有一条")))
                .put("/admin/product-releases/" + idOf("1.0.0"))
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", equalTo("操作失败,已发布的产品版本不能修改版本号或两端绑定"));
        assertThat(column("1.0.0", "frontend_version")).isEqualTo("0.0.18");

        authRequest(admin)
                .body(Map.of(
                        "version", "1.0.0",
                        "frontendVersion", "0.0.18",
                        "backendVersion", "1.0.52",
                        "title", "改过的标题",
                        "summary", "改过的摘要",
                        "fixed", List.of("修正了一处文案")))
                .put("/admin/product-releases/" + idOf("1.0.0"))
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(publishedAt("1.0.0")).isEqualTo(publishedAt);
        assertThat(column("1.0.0", "title")).isEqualTo("改过的标题");
    }

    @Test
    void shouldHideWithdrawnReleaseAndAllowThePairToBePublishedAgain() {
        String admin = createNormalAdmin();
        createTestUser(12L, "reader-two", "123456", "reader-two@example.com");
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);
        authRequest(admin)
                .post("/admin/product-releases/" + idOf("1.0.0") + "/withdraw")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        userRequest(loginAs("reader-two", "123456"))
                .get("/app/product-releases/current")
                .then()
                .body("data.release", nullValue());

        publish(admin, "1.4.0", "0.0.18", "1.0.52", true);
        assertThat(statusOf("1.4.0")).isEqualTo(1);
        assertThat(statusOf("1.0.0")).isEqualTo(2);
    }

    @Test
    void shouldShowOnlyTheLatestToANewUserAndAdvanceSeenForward() {
        String admin = createNormalAdmin();
        createTestUser(13L, "reader-three", "123456", "reader-three@example.com");
        createTestUser(14L, "reader-four", "123456", "reader-four@example.com");
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);
        publish(admin, "1.5.0", "0.0.21", "1.0.52", true);

        String first = loginAs("reader-three", "123456");
        userRequest(first)
                .get("/app/product-releases/current")
                .then()
                .body("data.release.version", equalTo("1.5.0"))
                .body("data.olderCount", equalTo(1))
                .body("data.release.frontendVersion", nullValue());

        userRequest(first)
                .post("/app/product-releases/" + idOf("1.0.0") + "/seen")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(first)
                .get("/app/product-releases/current")
                .then()
                .body("data.release.version", equalTo("1.5.0"));

        userRequest(first)
                .post("/app/product-releases/" + idOf("1.5.0") + "/seen")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(first)
                .get("/app/product-releases/current")
                .then()
                .body("data.release", nullValue());
        userRequest(first)
                .post("/app/product-releases/" + idOf("1.0.0") + "/seen")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(first)
                .get("/app/product-releases/current")
                .then()
                .body("data.release", nullValue());

        userRequest(loginAs("reader-four", "123456"))
                .get("/app/product-releases/")
                .then()
                .body("data.total", equalTo(2))
                .body("data.list[0].version", equalTo("1.5.0"))
                .body("data.list[0].frontendVersion", nullValue());
    }

    @Test
    void shouldDistinguishReleasesPublishedInTheSameSecond() {
        String admin = createNormalAdmin();
        createTestUser(16L, "reader-six", "123456", "reader-six@example.com");
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);

        String reader = loginAs("reader-six", "123456");
        userRequest(reader)
                .post("/app/product-releases/" + idOf("1.0.0") + "/seen")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        // 紧跟着发布的一条通常与上一条落在同一秒，按秒比较会把它误判为已读。
        publish(admin, "1.6.0", "0.0.22", "1.0.52", true);

        userRequest(reader)
                .get("/app/product-releases/current")
                .then()
                .body("data.release.version", equalTo("1.6.0"))
                .body("data.olderCount", equalTo(0));
    }

    @Test
    void shouldFilterListByStatusAndServeAdminDetail() {
        String admin = createNormalAdmin();
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);
        publish(admin, "1.1.0", "0.0.18", "1.0.52", false);

        authRequest(admin)
                .queryParam("status", "DRAFT")
                .get("/admin/product-releases/")
                .then()
                .body("data.total", equalTo(1))
                .body("data.list[0].version", equalTo("1.1.0"))
                .body("data.list[0].status", equalTo("DRAFT"))
                .body("data.list[0].frontendVersion", equalTo("0.0.18"));

        authRequest(admin)
                .queryParam("status", "PUBLISHED")
                .get("/admin/product-releases/")
                .then()
                .body("data.total", equalTo(1))
                .body("data.list[0].version", equalTo("1.0.0"));

        authRequest(admin)
                .get("/admin/product-releases/" + idOf("1.0.0"))
                .then()
                .body("data.version", equalTo("1.0.0"))
                .body("data.title", equalTo("标题1.0.0"))
                .body("data.summary", equalTo("摘要1.0.0"))
                .body("data.added[0]", equalTo("新增1.0.0"))
                .body("data.publishedAt", notNullValue());

        authRequest(admin)
                .get("/admin/product-releases/999999")
                .then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));

        authRequest(admin)
                .queryParam("status", "UNKNOWN")
                .get("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    @Test
    void shouldRejectPublishingDraftWithoutCopy() {
        String admin = createNormalAdmin();
        authRequest(admin)
                .body(Map.of(
                        "version", "3.0.0",
                        "frontendVersion", "0.0.1",
                        "backendVersion", "1.0.0",
                        "title", " ",
                        "summary", "摘要",
                        "added", List.of("一条新增"),
                        "publish", true))
                .post("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        authRequest(admin)
                .body(Map.of(
                        "version", "3.1.0",
                        "frontendVersion", "0.0.1",
                        "backendVersion", "1.0.0",
                        "publish", false))
                .post("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(admin)
                .post("/admin/product-releases/" + idOf("3.1.0") + "/publish")
                .then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", containsString("标题不能为空"));
        authRequest(admin)
                .post("/admin/product-releases/" + idOf("3.1.0") + "/withdraw")
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
    }

    @Test
    void shouldRejectRepeatedPublishAndWithdraw() {
        String admin = createNormalAdmin();
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);

        authRequest(admin)
                .post("/admin/product-releases/" + idOf("1.0.0") + "/publish")
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));

        authRequest(admin)
                .post("/admin/product-releases/" + idOf("1.0.0") + "/withdraw")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(admin)
                .post("/admin/product-releases/" + idOf("1.0.0") + "/withdraw")
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()));
        authRequest(admin)
                .body(Map.of(
                        "version", "1.0.0",
                        "frontendVersion", "0.0.18",
                        "backendVersion", "1.0.52",
                        "title", "标题",
                        "summary", "摘要",
                        "added", List.of("一条新增")))
                .put("/admin/product-releases/" + idOf("1.0.0"))
                .then()
                .body("code", equalTo(ResultCode.OPERATION_FAIL.getCode()))
                .body("msg", containsString("已撤回的产品版本不能修改"));
    }

    @Test
    void shouldListPublishedReleasesForUsersWithPaging() {
        String admin = createNormalAdmin();
        createTestUser(17L, "reader-seven", "123456", "reader-seven@example.com");
        publish(admin, "1.0.0", "0.0.18", "1.0.52", true);
        publish(admin, "1.1.0", "0.0.19", "1.0.52", true);
        publish(admin, "1.2.0", "0.0.20", "1.0.52", false);

        String reader = loginAs("reader-seven", "123456");
        userRequest(reader)
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 1)
                .get("/app/product-releases/")
                .then()
                .body("data.total", equalTo(2))
                .body("data.list.size()", equalTo(1))
                .body("data.list[0].version", equalTo("1.1.0"))
                .body("data.list[0].frontendVersion", nullValue());

        userRequest(reader)
                .queryParam("pageNum", 2)
                .queryParam("pageSize", 1)
                .get("/app/product-releases/")
                .then()
                .body("data.list[0].version", equalTo("1.0.0"));

        userRequest(reader)
                .get("/app/product-releases/current")
                .then()
                .body("data.release.version", equalTo("1.1.0"))
                .body("data.olderCount", equalTo(1));

        userRequest(reader)
                .post("/app/product-releases/999999/seen")
                .then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
    }

    @Test
    void shouldRejectInvalidVersionAnonymousAndNonAdminAccess() {
        createTestUser(15L, "reader-five", "123456", "reader-five@example.com");
        String admin = createNormalAdmin();
        authRequest(admin)
                .body(body("1.0", "0.0.1", "1.0.0", true))
                .post("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        request().get("/app/product-releases/current")
                .then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        userRequest(loginAs("reader-five", "123456"))
                .body(body("2.0.0", "0.0.1", "1.0.0", true))
                .post("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));

        authRequest(admin)
                .get("/admin/product-releases/baseline")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", nullValue());

        publish(admin, "2.0.0", "0.0.2", "1.0.0", true);
        authRequest(admin)
                .get("/admin/product-releases/baseline")
                .then()
                .body("data.frontendVersion", equalTo("0.0.2"))
                .body("data.backendVersion", equalTo("1.0.0"))
                .body("data.version", nullValue());

        userRequest(loginAs("reader-five", "123456"))
                .post("/app/product-releases/" + idOf("2.0.0") + "00/seen")
                .then()
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(admin)
                .post("/admin/product-releases/" + idOf("2.0.0") + "/withdraw")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        userRequest(loginAs("reader-five", "123456"))
                .post("/app/product-releases/" + idOf("2.0.0") + "/seen")
                .then()
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()))
                .body("msg", containsString("更新说明不存在或已撤回"));
    }

    @Test
    void shouldKeepDraftWithoutCopyAndListPublishedOnly() {
        String admin = createNormalAdmin();
        authRequest(admin)
                .body(Map.of(
                        "version", "0.1.0",
                        "frontendVersion", "0.0.1",
                        "backendVersion", "1.0.0",
                        "publish", false))
                .post("/admin/product-releases/")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data", notNullValue());

        userRequest(loginAs(createReader("draft-reader"), "123456"))
                .get("/app/product-releases/current")
                .then()
                .body("data.release", nullValue());
        authRequest(admin)
                .get("/admin/product-releases/")
                .then()
                .body("data.total", equalTo(1))
                .body("data.list[0].status", equalTo("DRAFT"))
                .body("data.list[0].frontendVersion", equalTo("0.0.1"));
    }

    private String createReader(String username) {
        long id = Math.abs(username.hashCode()) + 1000L;
        createTestUser(id, username, "123456", username + "@example.com");
        return username;
    }

    private void publish(String adminToken, String version, String frontend, String backend, boolean publish) {
        authRequest(adminToken)
                .body(body(version, frontend, backend, publish))
                .post("/admin/product-releases/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
    }

    private Map<String, Object> body(String version, String frontend, String backend, boolean publish) {
        return Map.of(
                "version", version,
                "frontendVersion", frontend,
                "backendVersion", backend,
                "title", "标题" + version,
                "summary", "摘要" + version,
                "added", List.of("新增" + version),
                "publish", publish
        );
    }

    private long idOf(String version) {
        return jdbcTemplate.queryForObject("SELECT id FROM product_release WHERE version = ?", Long.class, version);
    }

    private int statusOf(String version) {
        return jdbcTemplate.queryForObject("SELECT status FROM product_release WHERE version = ?", Integer.class, version);
    }

    private String column(String version, String column) {
        return jdbcTemplate.queryForObject("SELECT " + column + " FROM product_release WHERE version = ?", String.class, version);
    }

    private LocalDateTime publishedAt(String version) {
        return jdbcTemplate.queryForObject(
                "SELECT published_at FROM product_release WHERE version = ?", LocalDateTime.class, version);
    }
}

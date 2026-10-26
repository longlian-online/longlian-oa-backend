package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class StorageStatsApiTest extends BaseApiTest {

    @Test
    void shouldSummarizeStorageByOrganizationWithoutAvatars() {
        createOrganization(1L, "甲组");
        createOrganization(2L, "乙组");
        createAdmin(1L, "stats-admin", "123456", "normal");
        insertResource(1L, 1L, "cover", 10L, 1, 100L, null, null);
        insertResource(2L, 1L, "cover", 11L, 2, 40L, testNow().minusDays(3), null);
        insertResource(3L, 1L, "cover", 12L, 2, 7L, testNow().minusDays(40), testNow().minusDays(1));
        insertResource(4L, 1L, "cover", 0L, 0, 5L, null, null);
        insertResource(5L, 1L, "thumbnail", 13L, 1, 20L, null, null);
        insertResource(6L, 1L, "task_submit", 14L, 1, 30L, null, null);
        insertResource(7L, 1L, "avatar", 15L, 1, 999L, null, null);
        insertResource(8L, 2L, "cover", 16L, 1, 1L, null, null);

        authRequest(adminLoginAs("stats-admin", "123456"))
                .queryParam("orgName", "甲组")
                .queryParam("pageNum", 1)
                .queryParam("pageSize", 10)
                .get("/admin/storage-stats/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1))
                .body("data.list[0].name", equalTo("甲组"))
                .body("data.list[0].occupiedBytes", equalTo(195))
                .body("data.list[0].reclaimedBytes", equalTo(7))
                .body("data.list[0].cover.unbound.count", equalTo(1))
                .body("data.list[0].cover.unbound.bytes", equalTo(5))
                .body("data.list[0].cover.active.count", equalTo(1))
                .body("data.list[0].cover.active.bytes", equalTo(100))
                .body("data.list[0].cover.retention.count", equalTo(1))
                .body("data.list[0].cover.retention.bytes", equalTo(40))
                .body("data.list[0].cover.recycled.count", equalTo(1))
                .body("data.list[0].cover.recycled.bytes", equalTo(7))
                .body("data.list[0].thumbnail.active.bytes", equalTo(20))
                .body("data.list[0].thumbnail.retention.count", equalTo(0))
                .body("data.list[0].taskSubmit.active.bytes", equalTo(30))
                .body("data.list[0].avatar", nullValue());
    }

    @Test
    void shouldRejectStorageStatsWithoutAuth() {
        request().get("/admin/storage-stats/")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    @Test
    void shouldPhysicallyDeleteOnlyExpiredAbandonedFiles() {
        createOrganization(1L, "甲组");
        createAdmin(2L, "cleanup-root", "123456", "root");
        insertResource(11L, 1L, "cover", 21L, 2, 10L, testNow().minusDays(31), null);
        insertResource(12L, 1L, "thumbnail", 22L, 2, 10L, testNow().minusDays(1), null);

        authRequest(adminLoginAs("cleanup-root", "123456"))
                .body("{}")
                .post("/admin/scheduled-tasks/resource-cleanup/trigger")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(cleanedAt(11L)).isNotNull();
        assertThat(cleanedAt(12L)).isNull();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = 12", Integer.class)).isEqualTo(2);
    }

    private void insertResource(long id, long orgId, String bizType, long bizId, int status, long fileSize,
                                LocalDateTime deprecatedAt, LocalDateTime cleanedAt) {
        createResource(id, orgId, 1L);
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = ?, biz_id = ?, process_status = ?, file_size = ?, "
                        + "deprecated_at = ?, storage_cleaned_at = ?, storage_key = ? WHERE id = ?",
                bizType, bizId, status, fileSize, deprecatedAt, cleanedAt, bizType + "/" + id + ".png", id);
    }

    private LocalDateTime cleanedAt(long resourceId) {
        return jdbcTemplate.queryForObject(
                "SELECT storage_cleaned_at FROM resource WHERE id = ?", LocalDateTime.class, resourceId);
    }
}

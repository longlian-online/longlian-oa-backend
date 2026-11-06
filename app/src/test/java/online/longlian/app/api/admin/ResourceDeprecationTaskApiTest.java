package online.longlian.app.api.admin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class ResourceDeprecationTaskApiTest extends BaseApiTest {

    @Test
    void shouldDeprecateOnlyActivatedResourcesMissingFromCurrentBusinessReferences() {
        createUserWithOrganization(1L, "owner", "123456", "owner@example.com", 1L, 1L, "ORG_ADMIN");
        createAdmin(2L, "deprecation-admin", "123456", "root");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, creator_id) VALUES (1, 1, '漫画', 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, cover_file_id, thumbnail_file_id, creator_id) "
                        + "VALUES (1, 1, 1, '在用企划', 1, 2, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, cover_file_id, creator_id, deleted_at) "
                        + "VALUES (2, 1, 1, '已删企划', 3, 1, NOW())");
        jdbcTemplate.update("UPDATE `user` SET avatar_file_id = 4 WHERE id = 1");
        jdbcTemplate.update("UPDATE organization SET avatar_file_id = 6 WHERE id = 1");
        jdbcTemplate.update(
                "INSERT INTO project_attachment (id, project_id, file_id, uploader_id) VALUES (1, 1, 8, 1)");
        jdbcTemplate.update(
                "INSERT INTO project_attachment (id, project_id, file_id, uploader_id, deleted_at) "
                        + "VALUES (2, 1, 9, 1, NOW())");
        jdbcTemplate.update(
                "INSERT INTO task_instance (id, project_id, item_id, item_task_node_id, task_flow_id, status) "
                        + "VALUES (1, 1, 1, 1, 1, 3), (2, 1, 1, 1, 1, 3)");
        jdbcTemplate.update("UPDATE task_instance SET deleted_at = NOW() WHERE id = 2");
        jdbcTemplate.update(
                "INSERT INTO task_submission (id, project_id, item_id, task_instance_id, item_task_node_id, "
                        + "submitter_id, version_no, metadata, status) VALUES "
                        + "(1, 1, 1, 1, 1, 1, 1, ?, 3),"
                        + "(2, 1, 1, 1, 1, 1, 2, ?, 1),"
                        + "(3, 1, 1, 2, 1, 1, 1, ?, 1)",
                "{\"old\":{\"fileId\":\"10\"}}",
                "{\"file\":{\"fileId\":\"123\"}}",
                "{\"file\":{\"fileId\":\"13\"}}");
        for (long id : new long[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 12, 13, 14, 15, 123}) {
            createResource(id, 1L, 1L);
        }
        LocalDateTime keptDeprecatedAt = LocalDateTime.of(2026, 1, 2, 3, 4, 5);
        jdbcTemplate.update(
                "UPDATE resource SET process_status = ?, deprecated_at = ? WHERE id = 14",
                FileProcessStatus.Deprecated.getCode(), keptDeprecatedAt);
        jdbcTemplate.update("UPDATE resource SET process_status = ? WHERE id = 15",
                FileProcessStatus.Pending.getCode());

        authRequest(adminLoginAs("deprecation-admin", "123456"))
                .body("{}")
                .post("/admin/scheduled-tasks/resource-deprecation/trigger")
                .then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        for (long id : new long[]{1, 2, 4, 6, 8, 10, 123}) {
            assertThat(status(id)).as("resource %s stays activated", id)
                    .isEqualTo(FileProcessStatus.Activated.getCode());
        }
        for (long id : new long[]{3, 5, 7, 9, 12, 13}) {
            assertThat(status(id)).as("resource %s is deprecated", id)
                    .isEqualTo(FileProcessStatus.Deprecated.getCode());
            assertThat(deprecatedAt(id)).isNotNull();
        }
        assertThat(status(14)).isEqualTo(FileProcessStatus.Deprecated.getCode());
        assertThat(deprecatedAt(14)).isEqualTo(keptDeprecatedAt);
        assertThat(status(15)).isEqualTo(FileProcessStatus.Pending.getCode());
        LocalDateTime firstDeprecatedAt = deprecatedAt(3);

        authRequest(adminLoginAs("deprecation-admin", "123456"))
                .body("{}")
                .post("/admin/scheduled-tasks/resource-deprecation/trigger")
                .then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(deprecatedAt(3)).isEqualTo(firstDeprecatedAt);
        assertThat(deprecatedAt(14)).isEqualTo(keptDeprecatedAt);
    }

    private Integer status(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT process_status FROM resource WHERE id = ?", Integer.class, id);
    }

    private LocalDateTime deprecatedAt(long id) {
        return jdbcTemplate.queryForObject(
                "SELECT deprecated_at FROM resource WHERE id = ?", LocalDateTime.class, id);
    }
}

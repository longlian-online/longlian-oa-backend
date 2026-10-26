package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

public class OrgAdminPresetTagApiTest extends BaseApiTest {

    @Test
    void shouldSortUserOptionsByPriorityAndKeepAdminListByCreatedTime() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("""
                INSERT INTO organization_preset_tag
                    (id, org_id, name, color, priority, creator_id, created_at, deleted_at) VALUES
                (1,1,'默认',NULL,0,1,'2026-01-01',NULL),
                (2,1,'作者','blue',10,1,'2026-01-02',NULL),
                (3,1,'同优先级','green',10,1,'2026-01-03',NULL),
                (4,1,'负优先级','amber',-1,1,'2026-01-04',NULL),
                (5,2,'跨组织','rose',100,1,'2026-01-05',NULL),
                (6,1,'已删除','violet',100,1,'2026-01-06','2026-01-07')
                """);

        authRequest(token).get("/app/preset-tags").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.id", contains("2", "3", "1", "4"))
                .body("data.name", contains("作者", "同优先级", "默认", "负优先级"))
                .body("data.color", contains("blue", "green", null, "amber"));
        authRequest(token).get("/orgadmin/preset-tags?orderDir=DESC").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", contains("4", "3", "2", "1"))
                .body("data.list.priority", contains(-1, 10, 10, 0));
        authRequest(token).get("/orgadmin/preset-tags?keyword=作者").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.name", contains("作者"))
                .body("data.total", equalTo(1));
    }

    @Test
    void shouldCreateDuplicateNamesAndClearColorWithoutChangingOmittedPriority() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");

        authRequest(token).body(Map.of("name", "  加急  ")).post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", "加急", "color", "blue", "priority", 20))
                .post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_preset_tag WHERE name = '加急' AND deleted_at IS NULL",
                Integer.class)).isEqualTo(2);
        Long id = jdbcTemplate.queryForObject(
                "SELECT id FROM organization_preset_tag WHERE color = 'blue'", Long.class);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT priority FROM organization_preset_tag WHERE name = '加急' AND color IS NULL",
                Integer.class)).isZero();

        authRequest(token).body(Map.of("name", "作者")).put("/orgadmin/preset-tags/" + id)
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        assertThat(jdbcTemplate.queryForMap(
                "SELECT name, color, priority FROM organization_preset_tag WHERE id = ?", id))
                .containsEntry("name", "作者")
                .containsEntry("color", null)
                .containsEntry("priority", 20);
    }

    @Test
    void shouldRejectInvalidPresetTagInput() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        String name = "标".repeat(50);

        authRequest(token).body(Map.of("name", name, "color", "violet", "priority", -3))
                .post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", name + "签")).post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(token).body(Map.of("name", "   ")).post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(token).body(Map.of("name", "加急", "color", "red")).post("/orgadmin/preset-tags")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_preset_tag WHERE deleted_at IS NULL", Integer.class))
                .isEqualTo(1);
    }

    @Test
    void shouldDeletePresetTagWithoutChangingProjectMetadata() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_tag (id, org_id, name, color, priority, creator_id) VALUES (1, 1, '加急', 'amber', 0, 1)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id, metadata) VALUES (1, 1, 1, '作品', 1, ?)",
                "{\"tags\":[{\"key\":\"加急\",\"color\":\"amber\"}]}");

        authRequest(token).delete("/orgadmin/preset-tags/1").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).get("/app/preset-tags").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.size()", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .contains("加急");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM organization_preset_tag WHERE id = 1", Boolean.class))
                .isTrue();
    }

    @Test
    void shouldRejectPresetTagAccessWithoutAuthOrAdminRole() {
        request().get("/app/preset-tags").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().get("/orgadmin/preset-tags").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        createUserWithOrganization(1L, "member", "123456", "member@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("member", "123456");
        authRequest(token).get("/orgadmin/preset-tags").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).get("/app/preset-tags").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.size()", equalTo(0));
    }

    @Test
    void shouldRejectMissingAndCrossOrganizationPresetTags() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_tag (id, org_id, name, priority, creator_id) VALUES (9, 2, '别人的', 0, 1)");

        authRequest(token).body(Map.of("name", "改掉")).put("/orgadmin/preset-tags/9").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).delete("/orgadmin/preset-tags/99").then()
                .statusCode(200)
                .body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM organization_preset_tag WHERE id = 9", String.class)).isEqualTo("别人的");
    }
}

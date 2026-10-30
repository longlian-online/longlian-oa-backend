package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;

public class OrgAdminPresetFieldApiTest extends BaseApiTest {

    @Test
    void shouldListPresetFieldsByCreatedTimeWithCreatorNickname() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update("UPDATE `user` SET nickname = '' WHERE id = 1");
        jdbcTemplate.update("""
                INSERT INTO organization_preset_field
                    (id, org_id, name, creator_id, created_at, deleted_at) VALUES
                (1,1,'作者',1,'2026-01-01',NULL),
                (2,1,'状态',1,'2026-01-03',NULL),
                (3,2,'跨组织',1,'2026-01-04',NULL),
                (4,1,'已删除',1,'2026-01-05','2026-01-06')
                """);

        authRequest(token).get("/app/preset-fields").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.id", contains("2", "1"))
                .body("data.name", contains("状态", "作者"));
        authRequest(token).get("/orgadmin/preset-fields?orderDir=ASC").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", contains("1", "2"))
                .body("data.list.creatorNickname", contains(null, null))
                .body("data.list.createdAt", contains("2026-01-01 00:00:00", "2026-01-03 00:00:00"));
        jdbcTemplate.update("UPDATE `user` SET nickname = '管理员' WHERE id = 1");
        authRequest(token).get("/orgadmin/preset-fields?keyword=作者").then()
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.name", contains("作者"))
                .body("data.list[0].creatorNickname", equalTo("管理员"))
                .body("data.total", equalTo(1));
    }

    @Test
    void shouldCreateDuplicateNamesAndRejectBlankOrTooLongNames() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        String name = "字".repeat(50);

        authRequest(token).body(Map.of("name", "  作者  ")).post("/orgadmin/preset-fields")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", "作者")).post("/orgadmin/preset-fields")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", name)).post("/orgadmin/preset-fields")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", name + "段")).post("/orgadmin/preset-fields")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(token).body(Map.of("name", "   ")).post("/orgadmin/preset-fields")
                .then().statusCode(200).body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM organization_preset_field WHERE name = '作者' AND deleted_at IS NULL",
                Integer.class)).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT creator_id FROM organization_preset_field WHERE name = ?", Long.class, name)).isEqualTo(1L);
    }

    @Test
    void shouldUpdateNameAndRejectMissingOrCrossOrganizationFields() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (8, 1, '作者', 1), (9, 2, '别人的', 1)");

        authRequest(token).body(Map.of("name", "  原作  ")).put("/orgadmin/preset-fields/8")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).body(Map.of("name", "改掉")).put("/orgadmin/preset-fields/9")
                .then().statusCode(200).body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).delete("/orgadmin/preset-fields/99")
                .then().statusCode(200).body("code", equalTo(ResultCode.DATA_NOT_EXIT.getCode()));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM organization_preset_field WHERE id = 8", String.class)).isEqualTo("原作");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT name FROM organization_preset_field WHERE id = 9", String.class)).isEqualTo("别人的");
    }

    @Test
    void shouldDeletePresetFieldWithoutChangingSavedProjectFields() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (1, 1, '作者', 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type_custom_field (id, org_id, type_id, preset_field_id, required, sort) VALUES (1, 1, 1, 1, 1, 0)");
        jdbcTemplate.update(
                "INSERT INTO project (id, org_id, type_id, title, creator_id, metadata) VALUES (1, 1, 1, '作品', 1, ?)",
                "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"山田\"}]}");

        authRequest(token).delete("/orgadmin/preset-fields/1").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));

        authRequest(token).get("/app/preset-fields").then()
                .body("data.size()", equalTo(0));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .contains("作者");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM organization_preset_field WHERE id = 1", Boolean.class)).isTrue();
        assertThat(jdbcTemplate.queryForObject(
                "SELECT deleted_at IS NOT NULL FROM project_type_custom_field WHERE id = 1", Boolean.class)).isTrue();
    }

    @Test
    void shouldRejectPresetFieldAccessWithoutAuthOrAdminRole() {
        request().get("/app/preset-fields").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        request().get("/orgadmin/preset-fields").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));

        createUserWithOrganization(1L, "member", "123456", "member@example.com", 1L, 1L, "ORG_USER");
        String token = loginAs("member", "123456");
        authRequest(token).get("/orgadmin/preset-fields").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        authRequest(token).get("/app/preset-fields").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.size()", equalTo(0));
    }
}

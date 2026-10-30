package online.longlian.app.api.orgadmin;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

public class ProjectTypeCustomFormApiTest extends BaseApiTest {

    @Test
    void shouldSaveCustomFormAndExposeCurrentPresetNames() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (1, 1, '作者', 1), (2, 1, '状态', 1)");

        authRequest(token).body(Map.of(
                "name", "漫画",
                "customFields", List.of(
                        Map.of("presetFieldId", "2", "required", false),
                        Map.of("presetFieldId", "1", "required", true))))
                .post("/orgadmin/project-types")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long typeId = jdbcTemplate.queryForObject("SELECT id FROM project_type WHERE name = '漫画'", Long.class);

        authRequest(token).get("/orgadmin/project-types").then()
                .body("data.list[0].customFields.presetFieldId", contains("2", "1"))
                .body("data.list[0].customFields.name", contains("状态", "作者"))
                .body("data.list[0].customFields.required", contains(false, true));
        authRequest(token).get("/app/projects/types").then()
                .body("data[0].customFields.name", contains("状态", "作者"))
                .body("data[0].customFields.required", contains(false, true));

        jdbcTemplate.update("UPDATE organization_preset_field SET name = '原作' WHERE id = 1");
        authRequest(token).body(Map.of("name", "漫画")).put("/orgadmin/project-types/" + typeId)
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/app/projects/types").then()
                .body("data[0].customFields.name", contains("状态", "原作"));

        authRequest(token).body(Map.of("name", "漫画", "customFields", List.of()))
                .put("/orgadmin/project-types/" + typeId)
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/app/projects/types").then()
                .body("data[0].customFields", hasSize(0));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_type_custom_field WHERE type_id = ? AND deleted_at IS NULL",
                Integer.class, typeId)).isZero();
    }

    @Test
    void shouldSaveCustomNamedFieldsAlongsidePresetFields() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (1, 1, '作者', 1)");

        authRequest(token).body(Map.of(
                "name", "漫画",
                "customFields", List.of(
                        Map.of("presetFieldId", "1", "required", true),
                        Map.of("name", "备注", "required", false))))
                .post("/orgadmin/project-types")
                .then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long typeId = jdbcTemplate.queryForObject("SELECT id FROM project_type WHERE name = '漫画'", Long.class);

        authRequest(token).get("/orgadmin/project-types").then()
                .body("data.list[0].customFields.name", contains("作者", "备注"))
                .body("data.list[0].customFields.required", contains(true, false));
        authRequest(token).get("/app/projects/types").then()
                .body("data[0].customFields.name", contains("作者", "备注"))
                .body("data[0].customFields.required", contains(true, false));

        jdbcTemplate.update("UPDATE organization_preset_field SET name = '原作' WHERE id = 1");
        authRequest(token).get("/app/projects/types").then()
                .body("data[0].customFields.name", contains("原作", "备注"));

        authRequest(token).body(Map.of("name", "漫画", "customFields", List.of(
                Map.of("name", "  ", "required", true))))
                .put("/orgadmin/project-types/" + typeId)
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_type_custom_field WHERE type_id = ? AND deleted_at IS NULL",
                Integer.class, typeId)).isEqualTo(2);
    }

    @Test
    void shouldRejectDuplicateOrMissingPresetFields() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createOrganization(2L, "其他组织");
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (1, 1, '作者', 1), (9, 2, '别人的', 1)");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");

        authRequest(token).body(Map.of("name", "漫画", "customFields", List.of(
                Map.of("presetFieldId", "1", "required", true),
                Map.of("presetFieldId", "1", "required", false))))
                .put("/orgadmin/project-types/1")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        authRequest(token).body(Map.of("name", "漫画", "customFields", List.of(
                Map.of("presetFieldId", "9", "required", true))))
                .put("/orgadmin/project-types/1")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", equalTo("参数错误,预置表单字段不存在"));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM project_type_custom_field WHERE deleted_at IS NULL", Integer.class)).isZero();
    }

    @Test
    void shouldRejectProjectSaveUntilRequiredFieldsAreFilled() {
        createUserWithOrganization(1L, "orgadmin", "123456", "orgadmin@example.com", 1L, 1L, "ORG_ADMIN");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update(
                "UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = ?",
                FileProcessStatus.Uploaded.getCode(), 1L);
        String token = loginAs("orgadmin", "123456");
        jdbcTemplate.update(
                "INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        jdbcTemplate.update(
                "INSERT INTO organization_preset_field (id, org_id, name, creator_id) VALUES (1, 1, '作者', 1), (2, 1, '状态', 1)");
        jdbcTemplate.update("""
                INSERT INTO project_type_custom_field
                    (id, org_id, type_id, preset_field_id, required, sort) VALUES
                (1,1,1,1,1,0),
                (2,1,1,2,0,1)
                """);

        Map<String, Object> missing = projectBody("{\"tags\":[],\"fields\":[{\"name\":\"状态\",\"value\":\"连载\"}]}");
        authRequest(token).body(missing).post("/app/projects")
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()))
                .body("msg", equalTo("参数错误,请填写必填字段：作者"));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project", Integer.class)).isZero();

        authRequest(token).body(projectBody(
                "{\"tags\":[],\"fields\":[{\"name\":\" 作者 \",\"value\":\" 山田 \"},{\"name\":\"备注\",\"value\":\"\"}]}"))
                .post("/app/projects")
                .then().body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long projectId = jdbcTemplate.queryForObject("SELECT id FROM project WHERE title = '测试企划'", Long.class);

        authRequest(token).body(Map.of(
                "title", "测试企划",
                "alias", "alias",
                "typeId", "1",
                "metadata", "{\"tags\":[]}",
                "description", "测试描述"))
                .put("/app/projects/" + projectId)
                .then().body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = ?", String.class, projectId))
                .contains("作者");
    }

    private Map<String, Object> projectBody(String metadata) {
        return Map.of(
                "title", "测试企划",
                "alias", "alias",
                "typeId", "1",
                "metadata", metadata,
                "description", "测试描述",
                "coverFileId", "1");
    }
}

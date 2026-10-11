package online.longlian.app.api.app;

import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

class ProjectMetadataApiTest extends BaseApiTest {

    private String token;

    @BeforeEach
    void prepareProjectMetadataData() {
        createUserWithOrganization(1L, "metadatauser", "123456", "metadata@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = 1",
                FileProcessStatus.Uploaded.getCode());
        token = loginAs("metadatauser", "123456");
    }

    /** 创建时拒绝错误结构，不产生项目及工坊记录。 */
    @ParameterizedTest
    @ValueSource(strings = {"{", "[]", "{}", "{\"tags\":{}}", "{\"tags\":[null]}",
            "{\"tags\":[{\"key\":\"作者\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":1}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}"})
    void shouldRejectInvalidMetadataOnCreate(String metadata) {
        authRequest(token).body(createBody(metadata)).post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_workshop", Integer.class)).isZero();
    }

    /** 更新时拒绝错误结构，原 metadata 和标题不变。 */
    @ParameterizedTest
    @ValueSource(strings = {"null", "{\"tags\":null}", "{\"tags\":[\"text\"]}",
            "{\"tags\":[{\"value\":\"smile_snow\"}]}",
            "{\"tags\":[{\"key\":\" \",\"value\":\"\"}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":null}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}"})
    void shouldRejectInvalidMetadataOnUpdate(String metadata) {
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id) VALUES (1, 1, 1, '原标题', 'original', ?, 1)",
                "{\"tags\":[]}");
        authRequest(token).body(updateBody(metadata)).put("/app/projects/1").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo("{\"tags\":[]}");
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM project WHERE id = 1", String.class))
                .isEqualTo("原标题");
    }

    /** 创建及更新允许空 tags、空 value、重复 key，并原样保存顶层额外字段和格式。 */
    @ParameterizedTest
    @ValueSource(strings = {"{ \"tags\": [] }",
            "{\"note\":\"保留\",\"tags\":[{\"key\":\"作者\",\"value\":\"\"},{\"key\":\"作者\",\"value\":\"smile_snow\"}]}"})
    void shouldSaveValidMetadataUnchangedOnCreateAndUpdate(String metadata) {
        authRequest(token).body(createBody(metadata)).post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM project", Long.class);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = ?", String.class, id))
                .isEqualTo(metadata);
        jdbcTemplate.update("UPDATE project SET metadata = '{\"tags\":[]}' WHERE id = ?", id);
        authRequest(token).body(updateBody(metadata)).put("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.metadata", equalTo(metadata)).body("data.title", equalTo("更新标题"));
    }

    private Map<String, String> createBody(String metadata) {
        return Map.of("title", "创建标题", "alias", "ordinary", "typeId", "1",
                "metadata", metadata, "description", "简介", "coverFileId", "1");
    }

    private Map<String, String> updateBody(String metadata) {
        return Map.of("title", "更新标题", "alias", "ordinary", "metadata", metadata, "description", "简介");
    }
}

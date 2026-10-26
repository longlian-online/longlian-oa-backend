package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

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
            "{\"tags\":[{\"key\":\"作者\",\"value\":1}]}",
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}",
            "{\"tags\":[],\"fields\":{}}",
            "{\"tags\":[],\"fields\":[null]}",
            "{\"tags\":[],\"fields\":[{\"name\":\" \",\"value\":\"xxxx\"}]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"作者\"}]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":1}]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\",\"extra\":true}]}"})
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
            "{\"tags\":[{\"key\":\"作者\",\"value\":\"\",\"extra\":true}]}",
            "{\"tags\":[],\"fields\":null}",
            "{\"tags\":[],\"fields\":[{}]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"\",\"value\":\"xxxx\"}]}",
            "{\"tags\":[],\"fields\":[{\"value\":\"xxxx\"}]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":null}]}"})
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

    /** 新旧标签格式、预设颜色及顶层额外字段都应原样保存。 */
    @ParameterizedTest
    @ValueSource(strings = {"{ \"tags\": [] }",
            "{\"note\":\"保留\",\"tags\":[{\"key\":\"作者\",\"value\":\"\"},{\"key\":\"作者\",\"value\":\"smile_snow\"}]}",
            "{\"tags\":[{\"key\":\"单键\"},{\"key\":\"蓝色\",\"color\":\"blue\"},{\"key\":\"绿色\",\"value\":\"\",\"color\":\"green\"},{\"key\":\"琥珀色\",\"value\":\"内容\",\"color\":\"amber\"},{\"key\":\"玫瑰色\",\"color\":\"rose\"},{\"key\":\"紫色\",\"color\":\"violet\"}]}",
            "{\"tags\":[],\"fields\":[]}",
            "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\"},{\"name\":\"作者\",\"value\":\"\"}]}"})
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

    @ParameterizedTest
    @MethodSource("invalidColors")
    void shouldRejectInvalidColorsWithoutChangingProjectOrResource(Object color) {
        String metadata = "{\"tags\":[{\"key\":\"作者\",\"color\":" + JSON.toJSONString(color) + "}]}";
        authRequest(token).body(createBody(metadata)).post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project_workshop", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("SELECT biz_id FROM resource WHERE id = 1", Long.class)).isZero();

        String original = "{\"tags\":[{\"key\":\"作者\",\"color\":\"blue\"}]}";
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id) VALUES (1, 1, 1, '原标题', 'original', ?, 1)",
                original);
        authRequest(token).body(updateBody(metadata)).put("/app/projects/1").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM project WHERE id = 1", String.class))
                .isEqualTo("原标题");
    }

    private static Stream<Object> invalidColors() {
        return Stream.of(null, true, 1, Map.of("value", "blue"), List.of("blue"), "", "Blue", " blue ",
                "red", "#ffffff", "constructor", "blue; background: url(javascript:alert(1))", "blue".repeat(1000));
    }

    /** 调整数组顺序时，重复名称标签的值和颜色不能串项，搜索响应也保留完整标签。 */
    @Test
    void shouldPreserveTagAttributesThroughReorderingAndSearch() {
        Map<String, String> single = Map.of("key", "系列", "color", "amber");
        Map<String, String> pair = Map.of("key", "系列", "value", "星海", "color", "blue");
        Map<String, String> empty = Map.of("key", "系列", "value", "");
        String original = JSON.toJSONString(Map.of("tags", List.of(pair, single, empty)));
        authRequest(token).body(createBody(original)).post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM project", Long.class);

        List<Map<String, String>> reordered = List.of(single, empty, pair);
        String updated = JSON.toJSONString(Map.of("tags", reordered));
        authRequest(token).body(updateBody(updated)).put("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.metadata", equalTo(updated));
        authRequest(token).get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list[0].metadata", equalTo(updated));

        String searched = authRequest(token).queryParam("keyword", "星海")
                .get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).extract().jsonPath().getString("data.list[0].metadata");
        assertThat(JSON.parseObject(searched).getJSONArray("tags").toJavaList(Map.class))
                .containsExactly(pair, single, empty);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = ?", String.class, id))
                .isEqualTo(updated);
    }

    /** 自定义字段随创建、更新和详情原样往返，同组织成员可读。 */
    @Test
    void shouldPersistCustomFieldsForCreatorAndOrganizationMember() {
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 2");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");
        String created = "{\"tags\":[{\"key\":\"加急\",\"color\":\"amber\"}],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\"}]}";
        authRequest(token).body(createBody(created)).post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long id = jdbcTemplate.queryForObject("SELECT id FROM project", Long.class);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = ?", String.class, id))
                .isEqualTo(created);

        String memberToken = loginAs("member", "123456");
        authRequest(memberToken).get("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.metadata", equalTo(created));

        String updated = "{\"tags\":[],\"fields\":[{\"name\":\"译者\",\"value\":\"\"},{\"name\":\"作者\",\"value\":\"yyyy\"}]}";
        authRequest(token).body(updateBody(updated)).put("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        authRequest(token).get("/app/projects/" + id).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.metadata", equalTo(updated));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = ?", String.class, id))
                .isEqualTo(updated);
    }

    /** 非创建者不能改自定义字段，未登录不能创建。 */
    @Test
    void shouldRejectCustomFieldChangesWithoutPermission() {
        String original = "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\"}]}";
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id) VALUES (1, 1, 1, '原标题', 'original', ?, 1)",
                original);
        createTestUser(2L, "member", "123456", "member@example.com");
        jdbcTemplate.update("UPDATE `user` SET default_org_id = 1 WHERE id = 2");
        createOrganizationMember(2L, 1L, 2L, "ORG_USER");

        authRequest(loginAs("member", "123456")).body(updateBody(
                        "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"changed\"}]}"))
                .put("/app/projects/1").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED_OPERATION.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
        assertThat(jdbcTemplate.queryForObject("SELECT title FROM project WHERE id = 1", String.class))
                .isEqualTo("原标题");

        request().body(createBody("{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"xxxx\"}]}"))
                .post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM project", Integer.class)).isEqualTo(1);
    }

    private Map<String, String> createBody(String metadata) {
        return Map.of("title", "创建标题", "alias", "ordinary", "typeId", "1",
                "metadata", metadata, "description", "简介", "coverFileId", "1");
    }

    private Map<String, String> updateBody(String metadata) {
        return Map.of("title", "更新标题", "alias", "ordinary", "metadata", metadata, "description", "简介");
    }
}

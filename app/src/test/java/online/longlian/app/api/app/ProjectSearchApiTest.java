package online.longlian.app.api.app;

import com.alibaba.fastjson2.JSON;
import io.restassured.response.Response;
import online.longlian.app.api.BaseApiTest;
import online.longlian.app.common.result.ResultCode;
import online.longlian.common.enumeration.FileProcessStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;

class ProjectSearchApiTest extends BaseApiTest {

    private String token;

    @BeforeEach
    void prepareSearchData() {
        createUserWithOrganization(1L, "searchuser", "123456", "search@example.com", 1L, 1L, "ORG_USER");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (1, 1, '漫画', 1, 1)");
        token = loginAs("searchuser", "123456");
    }

    /** 中文子串按标题、别名、元数据的权重排序，多字段命中累加。 */
    @Test
    void shouldRankChineseMatchesByFieldWeightsAndCombineHits() {
        createProject(1, "星海远征", "Voyage", "{}");
        createProject(2, "银河旅行", "星海之旅", "{}");
        createProject(3, "遥远世界", "World", "{\"series\":\"星海\"}");
        createProject(4, "星海设定", "星海故事", "{\"series\":\"星海\"}");

        search(" 星海 ").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(4))
                .body("data.list.id", equalTo(List.of("4", "1", "2", "3")))
                .body("data.list[2].alias", equalTo("星海之旅"));
    }

    /** 英文全文相关性按 5/3/1 字段权重排序。 */
    @Test
    void shouldRankEnglishFulltextMatchesByFieldWeights() {
        createProject(1, "Database guide", "Guide", "{}");
        createProject(2, "Galaxy travel", "Database voyage", "{}");
        createProject(3, "Distant world", "World", "{\"series\":\"Database\"}");

        search("database").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
    }

    /** 同字段命中仍使用数据库原生相关性，不能退化为仅按时间排序。 */
    @Test
    void shouldUseNativeRelevanceBeforeRequestedTimeOrder() {
        createProject(1, "database database database reference", "one", "{}");
        createProject(2, "database introduction", "two", "{}");
        createProject(3, "unrelated astronomy", "three", "{}");
        jdbcTemplate.update("UPDATE project SET created_at = '2025-01-01' WHERE id = 1");
        jdbcTemplate.update("UPDATE project SET created_at = '2026-01-01' WHERE id = 2");
        List<Double> scores = jdbcTemplate.query(
                "SELECT MATCH(title) AGAINST('database' IN NATURAL LANGUAGE MODE) FROM project WHERE id IN (1, 2) ORDER BY id",
                (rs, rowNum) -> rs.getDouble(1));
        assertThat(scores.get(0)).isGreaterThan(scores.get(1));

        search("database").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("1", "2")));
    }

    /** 标题和别名完全匹配获得额外加分，短词也能入选。 */
    @Test
    void shouldBoostExactTitleAndAliasMatches() {
        createProject(1, "星海", "one", "{}");
        createProject(2, "银河旅行", "星海", "{}");
        createProject(3, "星海远征", "three", "{}");

        search("星海").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
    }

    /** 标题也命中时，返回的全部命中元数据仍稳定前置，数据库原值不变。 */
    @Test
    void shouldMoveMatchingObjectMetadataFirstWithoutChangingStoredJson() {
        String original = "{\"author\":\"山田\",\"series\":\"星海\",\"language\":\"中文\",\"tag\":\"星海\"}";
        createProject(1, "星海远征", "Voyage", original);

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1));
        String returned = response.jsonPath().getString("data.list[0].metadata");
        assertThat(JSON.parseObject(returned).keySet()).containsExactly("series", "tag", "author", "language");
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
    }

    /** 元数据数组中的命中元素整体前置，未命中元素维持原顺序。 */
    @Test
    void shouldMoveMatchingArrayMetadataFirst() {
        createProject(1, "遥远世界", "World",
                "[{\"label\":\"作者\",\"value\":\"山田\"},{\"label\":\"系列\",\"value\":\"星海\"},{\"label\":\"语言\",\"value\":\"中文\"}]");

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        var metadata = JSON.parseArray(response.jsonPath().getString("data.list[0].metadata"));
        assertThat(metadata.getJSONObject(0).getString("label")).isEqualTo("系列");
        assertThat(metadata.getJSONObject(1).getString("label")).isEqualTo("作者");
        assertThat(metadata.getJSONObject(2).getString("label")).isEqualTo("语言");
    }

    /** 搜索重排对象时，根字段和嵌套字段的显式 null 必须保留。 */
    @Test
    void shouldPreserveNullFieldsWhenReorderingObjectMetadata() {
        String original = "{\"author\":null,\"series\":\"星海\",\"publication\":{\"year\":null}}";
        createProject(1, "普通企划", "ordinary", original);

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        var metadata = JSON.parseObject(response.jsonPath().getString("data.list[0].metadata"));
        assertThat(metadata.keySet()).containsExactly("series", "author", "publication");
        assertThat(metadata).containsEntry("author", null);
        assertThat(metadata.getJSONObject("publication")).containsEntry("year", null);
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
    }

    /** 数组内命中和未命中的对象字段及数组 null 元素均应保留。 */
    @Test
    void shouldPreserveNullFieldsWhenReorderingArrayMetadata() {
        String original = "[{\"author\":null},{\"value\":\"星海\",\"optional\":null},null]";
        createProject(1, "普通企划", "ordinary", original);

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        var metadata = JSON.parseArray(response.jsonPath().getString("data.list[0].metadata"));
        assertThat(metadata.getJSONObject(0)).containsEntry("value", "星海").containsEntry("optional", null);
        assertThat(metadata.getJSONObject(1)).containsEntry("author", null);
        assertThat(metadata.get(2)).isNull();
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
    }

    /** 嵌套数字值可以被搜索，所属顶层元数据前置。 */
    @Test
    void shouldSearchNestedNumericMetadataAndMoveItsParentFirst() {
        createProject(1, "遥远世界", "World", "{\"author\":\"山田\",\"publication\":{\"year\":2025}}");

        Response response = search("2025");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata")).keySet())
                .containsExactly("publication", "author");
    }

    /** 元数据键名中的点号和引号不应与真实嵌套路径混淆。 */
    @Test
    void shouldPreserveSpecialMetadataKeysWhenMovingNestedMatches() {
        createProject(1, "普通企划", "ordinary",
                "{\"a.b\":\"other\",\"a\":{\"b\":\"星海\"},\"a\\\"b\":\"other\"}");

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata")).keySet())
                .containsExactly("a", "a.b", "a\"b");
    }

    /** 空元数据和历史空别名不影响标题命中，标量 JSON 保持原类型和内容。 */
    @Test
    void shouldHandleNullAndScalarMetadata() {
        createProject(1, "星海远征", null, null);
        createProject(2, "普通企划", "ordinary", "\"星海\"");
        createProject(3, "无元数据企划", "none", "[]");

        search("星海").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(2))
                .body("data.list.id", equalTo(List.of("1", "2")))
                .body("data.list[1].metadata", equalTo("\"星海\""));
    }

    /** 使用数据库命中路径处理大小写与重音，不依赖 Java 的字符串比较。 */
    @Test
    void shouldSearchMetadataIgnoringCaseAndAccents() {
        createProject(1, "遥远世界", "World", "{\"author\":\"山田\",\"series\":\"CAFÉ\"}");

        Response response = search("cafe");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata")).keySet())
                .containsExactly("series", "author");
    }

    /** 多词查询可命中元数据中的其中一个全文词，并前置该项。 */
    @Test
    void shouldSearchIndividualMetadataWordsInMultiwordQuery() {
        createProject(1, "遥远世界", "World", "{\"author\":\"山田\",\"series\":\"database\"}");

        Response response = search("database performance");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata")).keySet())
                .containsExactly("series", "author");
    }

    /** 相同内容放在标题、别名或元数据时，多词子串召回一致且按字段权重排序。 */
    @Test
    void shouldApplySameMultiwordSubstringRulesToAllSearchFields() {
        createProject(1, "foobar", "ordinary", "{}");
        createProject(2, "普通企划", "foobar", "{}");
        createProject(3, "普通企划", "ordinary", "{\"author\":\"山田\",\"series\":\"foobar\"}");
        createProject(4, "无匹配企划", "unrelated", "{}");

        Response response = search("foo bar");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[2].metadata")).keySet())
                .containsExactly("series", "author");
    }

    /** JSON 结构键不是业务元数据值，不能单独导致结果入选。 */
    @Test
    void shouldNotReturnProjectsMatchingOnlyMetadataKeys() {
        createProject(1, "遥远世界", "World", "{\"database\":\"山田\"}");

        search("database").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0)).body("data.list", hasSize(0));
    }

    /** 子串搜索支持单字、英文短词和全文停用词。 */
    @Test
    void shouldPreserveShortWordAndStopwordSearch() {
        createProject(1, "海洋日记", "go", "{\"series\":\"the\"}");

        for (String keyword : List.of("海", "go", "the")) {
            search(keyword).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.total", equalTo(1)).body("data.list[0].id", equalTo("1"));
        }
    }

    /** 通配符按字面值查询，不能扩大为所有企划。 */
    @Test
    void shouldTreatPercentUnderscoreAndEscapeAsLiteralCharacters() {
        createProject(1, "50%", "one", "{}");
        createProject(2, "ordinary", "two", "{\"series\":\"under_score\"}");
        createProject(3, "exclamation!", "three", "{}");
        createProject(4, "other", "four", "{}");

        List<String> keywords = List.of("%", "_", "!");
        for (int i = 0; i < keywords.size(); i++) {
            search(keywords.get(i)).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.total", equalTo(1))
                    .body("data.list[0].id", equalTo(Integer.toString(i + 1)));
        }
    }

    /** 组织作用域、禁用状态与软删除同时约束全部搜索分支。 */
    @Test
    void shouldExcludeOtherOrganizationsDisabledAndDeletedProjects() {
        for (int i = 1; i <= 4; i++) {
            createProject(i, "普通企划", "ordinary", "{\"series\":\"星海\"}");
        }
        jdbcTemplate.update("UPDATE project SET org_id = 2 WHERE id = 2");
        jdbcTemplate.update("UPDATE project SET resource_status = 0 WHERE id = 3");
        jdbcTemplate.update("UPDATE project SET deleted_at = NOW() WHERE id = 4");

        search("星海").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("1")));
    }

    /** 类型过滤同样作用于元数据命中结果。 */
    @Test
    void shouldFilterSearchResultsByProjectType() {
        createProject(1, "普通企划", "one", "{\"series\":\"星海\"}");
        createProject(2, "普通企划", "two", "{\"series\":\"星海\"}");
        jdbcTemplate.update("INSERT INTO project_type (id, org_id, name, status, creator_id) VALUES (2, 1, '小说', 1, 1)");
        jdbcTemplate.update("UPDATE project SET type_id = 2 WHERE id = 2");

        authRequest(token).queryParam("keyword", "星海").queryParam("projectType", "漫画")
                .get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("1")));
    }

    /** 数据库先排序再分页；超出末页时仍返回准确的总数。 */
    @Test
    void shouldPaginateRankedResultsAndKeepTotalOnEmptyPage() {
        createProject(1, "普通企划", "one", "{\"series\":\"星海\"}");
        createProject(2, "普通企划", "星海故事", "{}");
        createProject(3, "星海远征", "three", "{}");

        for (int page = 1; page <= 3; page++) {
            authRequest(token).queryParam("keyword", "星海").queryParam("pageSize", 1).queryParam("pageNum", page)
                    .get("/app/projects").then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.total", equalTo(3)).body("data.list", hasSize(1))
                    .body("data.list[0].id", equalTo(Integer.toString(4 - page)));
        }
        authRequest(token).queryParam("keyword", "星海").queryParam("pageSize", 1).queryParam("pageNum", 4)
                .get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3)).body("data.list", hasSize(0));
    }

    /** 同分按选择的更新时间和方向排序，时间相同再按 ID 排序。 */
    @Test
    void shouldUseRequestedTimeAndIdToBreakRelevanceTies() {
        for (int i = 1; i <= 3; i++) {
            createProject(i, "星海远征", "ordinary", "{}");
        }
        jdbcTemplate.update("UPDATE project SET updated_at = '2026-01-01' WHERE id IN (1, 2)");
        jdbcTemplate.update("UPDATE project SET updated_at = '2025-01-01' WHERE id = 3");

        authRequest(token).queryParam("keyword", "星海").queryParam("sortByTime", "UPDATE").queryParam("orderDir", "ASC")
                .get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("3", "1", "2")));
    }

    /** 空白关键词保留原有时间排序和元数据文本。 */
    @Test
    void shouldKeepOriginalListBehaviorWithoutKeyword() {
        String metadata = "{ \"author\": \"山田\", \"series\": \"星海\" }";
        createProject(1, "星海", "one", metadata);
        createProject(2, "普通企划", "two", "{}");
        jdbcTemplate.update("UPDATE project SET created_at = '2025-01-01' WHERE id = 1");
        jdbcTemplate.update("UPDATE project SET created_at = '2026-01-01' WHERE id = 2");

        search("  ").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("2", "1")))
                .body("data.list[1].metadata", equalTo(metadata));
    }

    /** 创建和修改后生成列与全文索引自动更新，无需额外同步或回填。 */
    @Test
    void shouldSearchCreatedAndUpdatedMetadataImmediately() {
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = 1",
                FileProcessStatus.Uploaded.getCode());
        authRequest(token).body(Map.of("title", "普通企划", "alias", "ordinary", "typeId", "1",
                        "metadata", "{\"series\":\"Oldtoken\"}", "description", "简介", "coverFileId", "1"))
                .post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long projectId = jdbcTemplate.queryForObject("SELECT id FROM project", Long.class);
        search("oldtoken").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(1));

        authRequest(token).body(Map.of("title", "普通企划", "alias", "ordinary",
                        "metadata", "{\"series\":\"Newtoken\"}", "description", "简介"))
                .put("/app/projects/" + projectId).then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        search("oldtoken").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(0));
        search("newtoken").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(1));
    }

    /** 无会话时不允许使用搜索接口。 */
    @Test
    void shouldRejectSearchWithoutAuthentication() {
        request().queryParam("keyword", "星海").get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.UNAUTHORIZED.getCode()));
    }

    /** 关键词超过上限时返回参数错误。 */
    @Test
    void shouldRejectOverlongSearchKeyword() {
        search("a".repeat(51)).then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 不存在的企划类型不应被静默忽略。 */
    @Test
    void shouldRejectUnknownProjectTypeDuringSearch() {
        authRequest(token).queryParam("keyword", "星海").queryParam("projectType", "不存在")
                .get("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.PARAM_ERROR.getCode()));
    }

    /** 注入文本使用绑定参数，不能扩大查询范围。 */
    @Test
    void shouldBindSearchKeywordAsData() {
        createProject(1, "普通企划", "ordinary", "{}");

        search("' OR 1=1 --").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(0)).body("data.list", hasSize(0));
    }

    private Response search(String keyword) {
        return authRequest(token).queryParam("keyword", keyword).get("/app/projects");
    }

    private void createProject(long id, String title, String alias, String metadata) {
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id, created_at, updated_at) "
                        + "VALUES (?, 1, 1, ?, ?, ?, 1, '2026-01-01', '2026-01-01')",
                id, title, alias, metadata);
    }
}

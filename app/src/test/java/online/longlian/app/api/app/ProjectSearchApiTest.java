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
        createProject(1, "星海远征", "Voyage", metadata());
        createProject(2, "银河旅行", "星海之旅", metadata());
        createProject(3, "遥远世界", "World", metadata("series", "星海"));
        createProject(4, "星海设定", "星海故事", metadata("series", "星海"));

        search(" 星海 ").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(4))
                .body("data.list.id", equalTo(List.of("4", "1", "2", "3")))
                .body("data.list[2].alias", equalTo("星海之旅"));
    }

    /** 英文全文相关性按 5/3/1 字段权重排序。 */
    @Test
    void shouldRankEnglishFulltextMatchesByFieldWeights() {
        createProject(1, "Database guide", "Guide", metadata());
        createProject(2, "Galaxy travel", "Database voyage", metadata());
        createProject(3, "Distant world", "World", metadata("series", "Database"));

        search("database").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
    }

    /** 同字段命中仍使用数据库原生相关性，不能退化为仅按时间排序。 */
    @Test
    void shouldUseNativeRelevanceBeforeRequestedTimeOrder() {
        createProject(1, "database database database reference", "one", metadata());
        createProject(2, "database introduction", "two", metadata());
        createProject(3, "unrelated astronomy", "three", metadata());
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
        createProject(1, "星海", "one", metadata());
        createProject(2, "银河旅行", "星海", metadata());
        createProject(3, "星海远征", "three", metadata());

        search("星海").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
    }

    /** 重复命中的 tags 稳定前置，顶层额外字段及数据库原值不变。 */
    @Test
    void shouldMoveMatchingTagsFirstWithoutChangingStoredJson() {
        String original = "{\"note\":\"保留\",\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"系列\",\"value\":\"星海\"},{\"key\":\"语言\",\"value\":\"中文\"},{\"key\":\"系列\",\"value\":\"星海\"}]}";
        createProject(1, "星海远征", "Voyage", original);

        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list", hasSize(1));
        var returned = JSON.parseObject(response.jsonPath().getString("data.list[0].metadata"));
        assertThat(returned.getString("note")).isEqualTo("保留");
        assertThat(returned.getJSONArray("tags").stream()
                .map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("key")))
                .containsExactly("系列", "系列", "作者", "语言");
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
    }

    /** 重排 tags 时保留顶层额外字段及其嵌套显式 null。 */
    @Test
    void shouldPreserveNullExtraFieldsWhenReorderingTags() {
        String original = "{\"note\":null,\"publication\":{\"year\":null},\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"系列\",\"value\":\"星海\"}]}";
        createProject(1, "普通企划", "ordinary", original);
        Response response = search("星海");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        var returned = JSON.parseObject(response.jsonPath().getString("data.list[0].metadata"));
        assertThat(returned).containsEntry("note", null);
        assertThat(returned.getJSONObject("publication")).containsEntry("year", null);
        assertThat(returned.getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("系列");
        assertThat(jdbcTemplate.queryForObject("SELECT metadata FROM project WHERE id = 1", String.class))
                .isEqualTo(original);
    }

    /** 仅命中 value，忽略 key 和顶层额外字段。 */
    @Test
    void shouldSearchOnlyTagValues() {
        createProject(1, "普通企划", "ordinary", "{\"note\":\"database\",\"tags\":[{\"key\":\"database\",\"value\":\"山田\"}]}");
        createProject(2, "遥远世界", "World", metadata("series", "database"));
        search("database").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("2")));
    }

    /** 空 tags 和空 value 不影响标题命中，也不产生额外元数据命中。 */
    @Test
    void shouldHandleEmptyTagsAndValues() {
        createProject(1, "星海远征", "one", "{\"tags\":[]}");
        createProject(2, "普通企划", "two", metadata("星海", ""));
        search("星海").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("1")))
                .body("data.list[0].metadata", equalTo("{\"tags\":[]}"));
    }

    /** 数据库匹配大小写与重音后，将对应 tag 前置。 */
    @Test
    void shouldSearchMetadataIgnoringCaseAndAccents() {
        createProject(1, "遥远世界", "World", metadata("author", "山田", "series", "CAFÉ"));
        Response response = search("cafe");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata"))
                .getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("series");
    }

    /** 多词查询可命中其中一个较长片段，并前置对应 tag。 */
    @Test
    void shouldSearchIndividualMetadataWordsInMultiwordQuery() {
        createProject(1, "遥远世界", "World", metadata("author", "山田", "series", "database"));
        Response response = search("database performance");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[0].metadata"))
                .getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("series");
    }

    /** 相同内容放在标题、别名或元数据时，多词子串召回一致且按字段权重排序。 */
    @Test
    void shouldApplySameMultiwordSubstringRulesToAllSearchFields() {
        createProject(1, "foobar", "ordinary", metadata());
        createProject(2, "普通企划", "foobar", metadata());
        createProject(3, "普通企划", "ordinary", metadata("author", "山田", "series", "foobar"));
        createProject(4, "无匹配企划", "unrelated", metadata());

        Response response = search("foo bar");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[2].metadata"))
                .getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("series");
    }

    /** 多个中文短词分别召回标题、别名和元数据，命中元数据稳定前置。 */
    @Test
    void shouldSearchMultipleShortChineseTermsAcrossAllFields() {
        createProject(1, "星海远征", "voyage", metadata());
        createProject(2, "普通企划", "银河之旅", metadata());
        createProject(3, "普通企划", "world", metadata("author", "山田", "series", "星海", "tag", "银河"));
        createProject(4, "山间日记", "diary", metadata());

        Response response = search("星海 银河");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[2].metadata"))
                .getJSONArray("tags").stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("key")))
                .containsExactly("series", "tag", "author");
    }

    /** 英文短词组合不依赖全文索引收录，保持字段权重及数组元数据顺序。 */
    @Test
    void shouldSearchMultipleShortEnglishTermsAcrossAllFields() {
        createProject(1, "Go project", "voyage", metadata());
        createProject(2, "普通企划", "OA system", metadata());
        createProject(3, "普通企划", "world", metadata("first", "other", "second", "Go language", "third", "OA tool"));
        createProject(4, "无匹配企划", "unrelated", metadata());

        Response response = search("go oa");
        response.then().statusCode(200).body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
        assertThat(JSON.parseObject(response.jsonPath().getString("data.list[2].metadata"))
                .getJSONArray("tags").stream().map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("value")))
                .containsExactly("Go language", "OA tool", "other");
    }

    /** 长词与短词混合查询时，短词不能在别名和元数据召回中被忽略。 */
    @Test
    void shouldSearchMixedLongAndShortTermsAcrossAllFields() {
        createProject(1, "Database guide", "guide", metadata());
        createProject(2, "普通企划", "OA system", metadata());
        createProject(3, "普通企划", "world", metadata("series", "OA"));
        createProject(4, "无匹配企划", "unrelated", metadata());

        search("database OA").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(3))
                .body("data.list.id", equalTo(List.of("1", "2", "3")));
    }


    /** 子串搜索支持单字、英文短词和全文停用词。 */
    @Test
    void shouldPreserveShortWordAndStopwordSearch() {
        createProject(1, "海洋日记", "go", metadata("series", "the"));

        for (String keyword : List.of("海", "go", "the")) {
            search(keyword).then().statusCode(200)
                    .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                    .body("data.total", equalTo(1)).body("data.list[0].id", equalTo("1"));
        }
    }

    /** 通配符按字面值查询，不能扩大为所有企划。 */
    @Test
    void shouldTreatPercentUnderscoreAndEscapeAsLiteralCharacters() {
        createProject(1, "50%", "one", metadata());
        createProject(2, "ordinary", "two", metadata("series", "under_score"));
        createProject(3, "exclamation!", "three", metadata());
        createProject(4, "other", "four", metadata());

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
            createProject(i, "普通企划", "ordinary", metadata("series", "星海"));
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
        createProject(1, "普通企划", "one", metadata("series", "星海"));
        createProject(2, "普通企划", "two", metadata("series", "星海"));
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
        createProject(1, "普通企划", "one", metadata("series", "星海"));
        createProject(2, "普通企划", "星海故事", metadata());
        createProject(3, "星海远征", "three", metadata());

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
            createProject(i, "星海远征", "ordinary", metadata());
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
        String metadata = "{ \"tags\": [{\"key\":\"series\",\"value\":\"星海\"}] }";
        createProject(1, "星海", "one", metadata);
        createProject(2, "普通企划", "two", metadata());
        jdbcTemplate.update("UPDATE project SET created_at = '2025-01-01' WHERE id = 1");
        jdbcTemplate.update("UPDATE project SET created_at = '2026-01-01' WHERE id = 2");

        search("  ").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.list.id", equalTo(List.of("2", "1")))
                .body("data.list[1].metadata", equalTo(metadata));
    }

    /** 创建和修改后直接搜索当前 tags 值，无需同步或回填。 */
    @Test
    void shouldSearchCreatedAndUpdatedMetadataImmediately() {
        createResource(1L, 1L, 1L);
        jdbcTemplate.update("UPDATE resource SET biz_type = 'cover', storage_key = 'cover/1.png', process_status = ? WHERE id = 1",
                FileProcessStatus.Uploaded.getCode());
        authRequest(token).body(Map.of("title", "普通企划", "alias", "ordinary", "typeId", "1",
                        "metadata", metadata("series", "Oldtoken"), "description", "简介", "coverFileId", "1"))
                .post("/app/projects").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()));
        Long projectId = jdbcTemplate.queryForObject("SELECT id FROM project", Long.class);
        search("oldtoken").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode())).body("data.total", equalTo(1));

        authRequest(token).body(Map.of("title", "普通企划", "alias", "ordinary",
                        "metadata", metadata("series", "Newtoken"), "description", "简介"))
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

    /** 注入文本使用绑定参数，仅返回合法 OR 子串命中，不能扩大到全部记录。 */
    @Test
    void shouldBindSearchKeywordAsData() {
        createProject(1, "普通企划", "ordinary", metadata());
        createProject(2, "无匹配企划", "neutral", metadata());

        search("' OR 1=1 --").then().statusCode(200)
                .body("code", equalTo(ResultCode.SUCCESS.getCode()))
                .body("data.total", equalTo(1)).body("data.list.id", equalTo(List.of("1")));
    }

    private Response search(String keyword) {
        return authRequest(token).queryParam("keyword", keyword).get("/app/projects");
    }

    private String metadata(String... entries) {
        var tags = new java.util.ArrayList<Map<String, String>>();
        for (int i = 0; i < entries.length; i += 2) {
            tags.add(Map.of("key", entries[i], "value", entries[i + 1]));
        }
        return JSON.toJSONString(Map.of("tags", tags));
    }

    private void createProject(long id, String title, String alias, String metadata) {
        jdbcTemplate.update("INSERT INTO project (id, org_id, type_id, title, alias, metadata, creator_id, created_at, updated_at) "
                        + "VALUES (?, 1, 1, ?, ?, ?, 1, '2026-01-01', '2026-01-01')",
                id, title, alias, metadata);
    }
}

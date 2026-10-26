package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class ProjectSearchSupportTest {

    @Test
    void buildParams_preservesFiltersAndEscapesLiteralWildcards() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .orgId(7L).keyword("  50%_!  ")
                .sortByTime(SortByTime.UPDATE).orderDir(SortDirection.ASC).build(), 9L);

        assertThat(result.getOrgId()).isEqualTo(7L);
        assertThat(result.getTypeId()).isEqualTo(9L);
        assertThat(result.getKeyword()).isEqualTo("50%_!");
        assertThat(result.getMatchPattern()).isEqualTo("%50!%!_!!%");
        assertThat(result.getTagMatchPattern()).isEqualTo("50!%!_!!");
        assertThat(result.getSortByTime()).isEqualTo(SortByTime.UPDATE);
        assertThat(result.getOrderDir()).isEqualTo(SortDirection.ASC);
    }

    /** 内部空白和重复词按原样保留，不生成独立词的搜索模式。 */
    @Test
    void shouldPreserveWholeKeywordWithoutSplitting() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .keyword("  星海　银河\tGo OA 星海  ").build(), null);

        assertThat(result.getKeyword()).isEqualTo("星海　银河\tGo OA 星海");
        assertThat(result.getMatchPattern()).isEqualTo("%星海　银河\tGo OA 星海%");
        assertThat(result.getTagMatchPattern()).isEqualTo("星海　银河\tGo OA 星海");
    }

    /** 多词关键词中的通配符按字面转义，标题别名和 tag 共用完整关键词。 */
    @Test
    void shouldEscapeLiteralWildcardsInWholeMultiwordKeyword() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .keyword("50%_! Go").build(), null);

        assertThat(result.getMatchPattern()).isEqualTo("%50!%!_!! Go%");
        assertThat(result.getTagMatchPattern()).isEqualTo("50!%!_!! Go");
    }

    /** 命中项稳定前置，重复路径不重复输出，保留顶层额外字段。 */
    @Test
    void shouldMoveMatchingTagsStably() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"note\":\"保留\",\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"系列\",\"value\":\"星海\"},{\"key\":\"语言\",\"value\":\"中文\"},{\"key\":\"系列\",\"value\":\"星海\"}]}",
                "[\"$.tags[3].value\",\"$.tags[1].value\",\"$.tags[1].value\"]");
        var object = JSON.parseObject(result);
        assertThat(object.getString("note")).isEqualTo("保留");
        assertThat(object.getJSONArray("tags").toJavaList(Map.class)).containsExactly(
                Map.of("key", "系列", "value", "星海"),
                Map.of("key", "系列", "value", "星海"),
                Map.of("key", "作者", "value", "山田"),
                Map.of("key", "语言", "value", "中文"));
    }

    @Test
    void shouldMoveKeyAndValueMatchesOnceWhilePreservingTagAttributes() {
        String original = "{\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"重点\",\"color\":\"amber\"},{\"key\":\"重点\",\"value\":\"重点\",\"color\":\"rose\"},{\"key\":\"分类\",\"value\":\"重点\"},{\"key\":\"语言\",\"value\":\"中文\"}]}";
        String result = ProjectSearchSupport.prioritizeMetadata(original,
                "[\"$.tags[3].value\",\"$.tags[2].value\",\"$.tags[2].key\",\"$.tags[1].key\"]");
        var tags = JSON.parseObject(original).getJSONArray("tags");
        assertThat(JSON.parseObject(result).getJSONArray("tags"))
                .containsExactly(tags.get(1), tags.get(2), tags.get(3), tags.get(0), tags.get(4));
    }

    /** 无命中时保留原始 JSON 格式。 */
    @Test
    void shouldPreserveNonMatchingMetadata() {
        String original = "{ \"tags\": [] }";
        assertThat(ProjectSearchSupport.prioritizeMetadata(original, "[]")).isEqualTo(original);
    }

    /** 第十项命中不能误将第一项前置。 */
    @Test
    void shouldNotConfuseTagIndices() {
        var original = IntStream.rangeClosed(0, 10)
                .mapToObj(i -> Map.of("key", "tag", "value", Integer.toString(i))).toList();
        String result = ProjectSearchSupport.prioritizeMetadata(
                JSON.toJSONString(Map.of("tags", original)), "[\"$.tags[10].value\"]");
        assertThat(JSON.parseObject(result).getJSONArray("tags").stream()
                .map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("value")))
                .containsExactly("10", "0", "1", "2", "3", "4", "5", "6", "7", "8", "9");
    }

    /** 只命中自定义字段时不改 tags 或 fields 的保存顺序。 */
    @Test
    void shouldKeepCustomFieldsAndTagOrderWhenOnlyFieldsMatch() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"fields\":[{\"name\":\"作者\",\"value\":\"星海\"},{\"name\":\"语言\",\"value\":\"中文\"}],\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"系列\",\"value\":\"星海\"}]}",
                "[\"$.fields[0].value\"]");
        var object = JSON.parseObject(result);
        assertThat(object.getJSONArray("tags").stream()
                .map(tag -> ((com.alibaba.fastjson2.JSONObject) tag).getString("key")))
                .containsExactly("作者", "系列");
        assertThat(object.getJSONArray("fields").stream()
                .map(field -> ((com.alibaba.fastjson2.JSONObject) field).getString("name")))
                .containsExactly("作者", "语言");
    }

    /** tag 前置时自定义字段仍保持原顺序。 */
    @Test
    void shouldReorderTagsWithoutMovingCustomFields() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"tags\":[{\"key\":\"作者\",\"value\":\"山田\"},{\"key\":\"系列\",\"value\":\"星海\"}],\"fields\":[{\"name\":\"作者\",\"value\":\"星海\"},{\"name\":\"语言\",\"value\":\"中文\"}]}",
                "[\"$.tags[1].value\",\"$.fields[0].value\"]");
        var object = JSON.parseObject(result);
        assertThat(object.getJSONArray("tags").getJSONObject(0).getString("key")).isEqualTo("系列");
        assertThat(object.getJSONArray("fields").getJSONObject(0).getString("name")).isEqualTo("作者");
        assertThat(object.getJSONArray("fields").getJSONObject(1).getString("value")).isEqualTo("中文");
    }
}

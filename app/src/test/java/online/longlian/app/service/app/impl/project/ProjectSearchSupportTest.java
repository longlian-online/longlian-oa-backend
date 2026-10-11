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
        assertThat(result.getMatchPatterns()).containsExactly("%50!%!_!!%");
        assertThat(result.getSortByTime()).isEqualTo(SortByTime.UPDATE);
        assertThat(result.getOrderDir()).isEqualTo(SortDirection.ASC);
    }

    @Test
    void buildParams_collectsDistinctLongAndShortTerms() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .orgId(1L).keyword("database performance database go").build(), null);

        assertThat(result.getMatchPatterns()).containsExactly(
                "%database performance database go%", "%database%", "%performance%", "%go%");
        assertThat(ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .keyword("星海").build(), null).getMatchPatterns()).containsExactly("%星海%");
    }

    @Test
    void buildParams_preservesShortTermsAcrossUnicodeWhitespace() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .keyword("星海　银河\tGo OA 星海").build(), null);

        assertThat(result.getMatchPatterns()).containsExactly(
                "%星海　银河\tGo OA 星海%", "%星海%", "%银河%", "%Go%", "%OA%");
    }

    @Test
    void buildParams_preservesLiteralWildcardsInShortMultiwordTerms() {
        ProjectSearchParamsBO result = ProjectSearchSupport.buildParams(ProjectListParamsBO.builder()
                .keyword("50%_! Go").build(), null);

        assertThat(result.getMatchPatterns()).containsExactly("%50!%!_!! Go%", "%50!%!_!!%", "%Go%");
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
}

package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;
import org.junit.jupiter.api.Test;

import java.util.List;
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

    @Test
    void prioritizeMetadata_movesAllMatchingObjectEntriesStably() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"author\":\"山田\",\"series\":\"星海\",\"language\":\"中文\",\"tag\":\"星海\"}",
                "[\"$[3]\",\"$[1]\",\"$[1]\"]");

        assertThat(JSON.parseObject(result).keySet()).containsExactly("series", "tag", "author", "language");
        assertThat(JSON.parseObject(result).getString("author")).isEqualTo("山田");
    }

    @Test
    void prioritizeMetadata_preservesNullObjectFieldsIncludingNestedValues() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"author\":null,\"series\":\"星海\",\"publication\":{\"year\":null}}",
                "[\"$[1]\"]");

        var object = JSON.parseObject(result);
        assertThat(object.keySet()).containsExactly("series", "author", "publication");
        assertThat(object).containsEntry("author", null);
        assertThat(object.getJSONObject("publication")).containsEntry("year", null);
    }

    @Test
    void prioritizeMetadata_preservesNullFieldsInMatchingAndNonMatchingArrayObjects() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "[{\"author\":null},{\"value\":\"星海\",\"optional\":null},null]",
                "[\"$[1].value\"]");

        var array = JSON.parseArray(result);
        assertThat(array.getJSONObject(0)).containsEntry("value", "星海").containsEntry("optional", null);
        assertThat(array.getJSONObject(1)).containsEntry("author", null);
        assertThat(array.get(2)).isNull();
    }

    @Test
    void prioritizeMetadata_movesNestedArrayMatchesWithoutConfusingIndices() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "[{\"value\":\"A\"},{\"value\":\"B\"},{\"nested\":{\"year\":2025}}]",
                "[\"$[2].nested.year\"]");

        assertThat(JSON.parseArray(result).getJSONObject(0).getJSONObject("nested").getInteger("year"))
                .isEqualTo(2025);
        assertThat(JSON.parseArray(result).getJSONObject(1).getString("value")).isEqualTo("A");
        assertThat(JSON.parseArray(result).getJSONObject(2).getString("value")).isEqualTo("B");
    }

    @Test
    void prioritizeMetadata_preservesNonMatchingInputAndScalarMetadata() {
        String original = "{ \"author\": \"山田\" }";
        assertThat(ProjectSearchSupport.prioritizeMetadata(original, "[]")).isEqualTo(original);
        assertThat(ProjectSearchSupport.prioritizeMetadata(null, "[]")).isNull();
        assertThat(ProjectSearchSupport.prioritizeMetadata("\"星海\"", "[\"$\"]")).isEqualTo("\"星海\"");
    }

    @Test
    void prioritizeMetadata_respectsPathBoundariesAndEscapedKeys() {
        String result = ProjectSearchSupport.prioritizeMetadata(
                "{\"abc\":\"one\",\"abcd\":\"two\",\"a\\\"b\":\"three\"}",
                JSON.toJSONString(List.of("$[1]", "$[2]")));

        assertThat(JSON.parseObject(result).keySet()).containsExactly("abcd", "a\"b", "abc");
    }

    @Test
    void prioritizeMetadata_doesNotConfuseFirstAndTenthArrayIndices() {
        List<Integer> original = IntStream.rangeClosed(0, 10).boxed().toList();

        String result = ProjectSearchSupport.prioritizeMetadata(JSON.toJSONString(original), "[\"$[10]\"]");

        assertThat(JSON.parseArray(result, Integer.class)).containsExactly(10, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9);
    }
}

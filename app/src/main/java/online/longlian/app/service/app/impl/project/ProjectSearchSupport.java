package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

public final class ProjectSearchSupport {

    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}_]+");
    private static final Pattern SEARCH_TERM = Pattern.compile("\\S+", Pattern.UNICODE_CHARACTER_CLASS);

    private ProjectSearchSupport() {
    }

    public static ProjectSearchParamsBO buildParams(ProjectListParamsBO params, Long typeId) {
        String keyword = params.getKeyword().trim();
        Set<String> terms = new LinkedHashSet<>();
        terms.add(keyword);
        // 子串召回保留短词和字面通配符
        SEARCH_TERM.matcher(keyword).results()
                .map(match -> match.group())
                .forEach(terms::add);
        WORD.matcher(keyword).results()
                .map(match -> match.group())
                .filter(term -> term.codePointCount(0, term.length()) >= 3)
                .forEach(terms::add);
        return ProjectSearchParamsBO.builder()
                .orgId(params.getOrgId())
                .typeId(typeId)
                .keyword(keyword)
                .matchPatterns(terms.stream().map(ProjectSearchSupport::likePattern).toList())
                .sortByTime(params.getSortByTime())
                .orderDir(params.getOrderDir())
                .build();
    }

    private static String likePattern(String text) {
        return "%" + text.replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
    }

    public static String prioritizeMetadata(String metadata, String matchedPaths) {
        List<String> paths = JSON.parseArray(matchedPaths, String.class);
        if (paths.isEmpty()) {
            return metadata;
        }
        Object parsed = JSON.parse(metadata);
        if (parsed instanceof JSONObject object) {
            List<Map.Entry<String, Object>> entries = new ArrayList<>(object.entrySet());
            JSONObject ordered = new JSONObject();
            orderedIndices(entries.size(), paths).forEach(index -> {
                Map.Entry<String, Object> entry = entries.get(index);
                ordered.put(entry.getKey(), entry.getValue());
            });
            return JSON.toJSONString(ordered, JSONWriter.Feature.WriteNulls);
        }
        if (parsed instanceof JSONArray array) {
            JSONArray ordered = new JSONArray();
            orderedIndices(array.size(), paths).forEach(index -> ordered.add(array.get(index)));
            return JSON.toJSONString(ordered, JSONWriter.Feature.WriteNulls);
        }
        return metadata;
    }

    private static List<Integer> orderedIndices(int size, List<String> paths) {
        // 对象的值投影为数组，以数据库命中路径排序，避免键名及字符比较规则的歧义。
        return IntStream.range(0, size).boxed()
                .sorted(Comparator.comparing(index -> !matches(paths, "$[" + index + "]")))
                .toList();
    }

    private static boolean matches(List<String> paths, String prefix) {
        return paths.stream().anyMatch(path -> path.equals(prefix)
                || path.startsWith(prefix + ".") || path.startsWith(prefix + "["));
    }
}

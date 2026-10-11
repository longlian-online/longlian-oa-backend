package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONWriter;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

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
        JSONObject object = JSON.parseObject(metadata);
        JSONArray tags = object.getJSONArray("tags");
        Set<String> matched = new LinkedHashSet<>(paths);
        JSONArray ordered = new JSONArray();
        for (int i = 0; i < tags.size(); i++) {
            if (matched.contains("$.tags[" + i + "].value")) {
                ordered.add(tags.get(i));
            }
        }
        for (int i = 0; i < tags.size(); i++) {
            if (!matched.contains("$.tags[" + i + "].value")) {
                ordered.add(tags.get(i));
            }
        }
        object.put("tags", ordered);
        return JSON.toJSONString(object, JSONWriter.Feature.WriteNulls);
    }
}

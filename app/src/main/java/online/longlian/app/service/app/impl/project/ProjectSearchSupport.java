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

public final class ProjectSearchSupport {

    private ProjectSearchSupport() {
    }

    public static ProjectSearchParamsBO buildParams(ProjectListParamsBO params, Long typeId) {
        String keyword = params.getKeyword().trim();
        return ProjectSearchParamsBO.builder()
                .orgId(params.getOrgId())
                .typeId(typeId)
                .keyword(keyword)
                .matchPattern(likePattern(keyword))
                .tagMatchPattern(escapePattern(keyword))
                .sortByTime(params.getSortByTime())
                .orderDir(params.getOrderDir())
                .build();
    }

    private static String likePattern(String text) {
        return "%" + escapePattern(text) + "%";
    }

    private static String escapePattern(String text) {
        return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
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
            if (matchesTag(matched, i)) {
                ordered.add(tags.get(i));
            }
        }
        for (int i = 0; i < tags.size(); i++) {
            if (!matchesTag(matched, i)) {
                ordered.add(tags.get(i));
            }
        }
        object.put("tags", ordered);
        return JSON.toJSONString(object, JSONWriter.Feature.WriteNulls);
    }

    private static boolean matchesTag(Set<String> matched, int index) {
        String path = "$.tags[" + index + "].";
        return matched.contains(path + "key") || matched.contains(path + "value");
    }
}

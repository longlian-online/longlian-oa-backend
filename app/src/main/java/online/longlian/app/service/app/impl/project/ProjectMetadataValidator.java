package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

import java.util.Set;

final class ProjectMetadataValidator {

    private static final Set<String> TAG_COLORS = Set.of("blue", "green", "amber", "rose", "violet");

    private ProjectMetadataValidator() {
    }

    static void validate(String metadata) {
        Object parsed;
        try {
            parsed = JSON.parse(metadata);
        } catch (JSONException exception) {
            throw new AppException(ResultCode.PARAM_ERROR, "metadata 必须是合法 JSON");
        }
        if (!(parsed instanceof JSONObject object)) {
            throw new AppException(ResultCode.PARAM_ERROR, "metadata 必须是对象");
        }
        if (!(object.get("tags") instanceof JSONArray tags)) {
            throw new AppException(ResultCode.PARAM_ERROR, "metadata.tags 必须是数组");
        }
        for (int i = 0; i < tags.size(); i++) {
            String path = "metadata.tags[" + i + "]";
            if (!(tags.get(i) instanceof JSONObject tag)) {
                throw new AppException(ResultCode.PARAM_ERROR, path + " 必须是对象");
            }
            validateTag(tag, path);
        }
    }

    private static void validateTag(JSONObject tag, String path) {
        if (!(tag.get("key") instanceof String key) || key.isBlank()) {
            throw new AppException(ResultCode.PARAM_ERROR, path + ".key 必须是非空白字符串");
        }
        if (tag.containsKey("value") && !(tag.get("value") instanceof String)) {
            throw new AppException(ResultCode.PARAM_ERROR, path + ".value 必须是字符串");
        }
        validateColor(tag, path);
        int allowedFieldCount = 1 + (tag.containsKey("value") ? 1 : 0)
                + (tag.containsKey("color") ? 1 : 0);
        if (tag.size() != allowedFieldCount) {
            throw new AppException(ResultCode.PARAM_ERROR, path + " 只能包含 key、value 和 color");
        }
    }

    private static void validateColor(JSONObject tag, String path) {
        if (!tag.containsKey("color")) {
            return;
        }
        if (!(tag.get("color") instanceof String color)
                || color.length() > 6 || !TAG_COLORS.contains(color)) {
            throw new AppException(ResultCode.PARAM_ERROR,
                    path + ".color 必须是 blue、green、amber、rose 或 violet");
        }
    }
}

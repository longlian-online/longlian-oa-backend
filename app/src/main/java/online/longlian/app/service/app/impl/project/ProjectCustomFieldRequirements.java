package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ProjectCustomFieldRequirements {

    private ProjectCustomFieldRequirements() {
    }

    static void requireSatisfied(String metadata, List<String> requiredNames) {
        if (requiredNames.isEmpty()) {
            return;
        }
        Set<String> filled = filledNames(metadata);
        List<String> missing = new ArrayList<>();
        for (String name : requiredNames) {
            if (!filled.contains(name) && !missing.contains(name)) {
                missing.add(name);
            }
        }
        if (!missing.isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "请填写必填字段：" + String.join("、", missing));
        }
    }

    private static Set<String> filledNames(String metadata) {
        Object parsed = JSON.parse(metadata);
        if (!(parsed instanceof JSONObject object) || !(object.get("fields") instanceof JSONArray fields)) {
            return Set.of();
        }
        Set<String> names = new HashSet<>();
        for (int index = 0; index < fields.size(); index++) {
            if (!(fields.get(index) instanceof JSONObject field)) {
                continue;
            }
            if (!(field.get("name") instanceof String name) || !(field.get("value") instanceof String value)) {
                continue;
            }
            if (!value.isBlank()) {
                names.add(name.trim());
            }
        }
        return names;
    }
}

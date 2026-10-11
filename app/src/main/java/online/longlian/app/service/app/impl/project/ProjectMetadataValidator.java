package online.longlian.app.service.app.impl.project;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONException;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

final class ProjectMetadataValidator {

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
            if (!(tag.get("key") instanceof String key) || key.isBlank()) {
                throw new AppException(ResultCode.PARAM_ERROR, path + ".key 必须是非空白字符串");
            }
            if (!(tag.get("value") instanceof String)) {
                throw new AppException(ResultCode.PARAM_ERROR, path + ".value 必须是字符串");
            }
            if (tag.size() != 2) {
                throw new AppException(ResultCode.PARAM_ERROR, path + " 只能包含 key 和 value");
            }
        }
    }
}

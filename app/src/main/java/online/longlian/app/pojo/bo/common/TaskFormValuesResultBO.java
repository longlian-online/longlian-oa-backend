package online.longlian.app.pojo.bo.common;

import com.alibaba.fastjson2.JSON;

import java.util.List;
import java.util.Map;

/** Normalized submission data and distinct resource IDs for the enclosing submission transaction. */
public record TaskFormValuesResultBO(Map<String, Object> values, List<Long> resourceIds) {
    /** Persists business values only; transient file URLs and display data are not stored. */
    public String serialize() {
        return JSON.toJSONString(values);
    }
}

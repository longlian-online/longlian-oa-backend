package online.longlian.app.service.common;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.TaskFormField;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** The task HTTP boundary and stored snapshots share one strict form contract. */
@Component
public class TaskFormService {
    private static final Set<String> TYPES = Set.of("text", "textarea", "file", "number", "select");
    private static final Set<String> FIELD_KEYS = Set.of("key", "label", "type", "required", "options");

    public List<TaskFormField> validateFields(List<TaskFormField> fields) {
        if (fields == null) {
            throw invalid("提交字段不能为空");
        }
        Set<String> keys = new HashSet<>();
        for (TaskFormField field : fields) {
            if (field == null || field.key() == null || field.key().isBlank()
                    || !keys.add(field.key()) || field.label() == null || field.label().isBlank()
                    || !TYPES.contains(field.type() == null ? "" : field.type())
                    || field.required() == null || field.options() == null) {
                throw invalid("提交字段定义无效");
            }
            Set<String> options = new HashSet<>();
            for (String option : field.options()) {
                if (option == null || option.isBlank() || !options.add(option)) {
                    throw invalid("选项定义无效");
                }
            }
            if (field.type().equals("select") ? options.isEmpty() : !options.isEmpty()) {
                throw invalid("选项仅用于选择字段且不能为空");
            }
        }
        return List.copyOf(fields);
    }

    public String serializeFields(List<TaskFormField> fields) {
        return JSON.toJSONString(validateFields(fields));
    }

    public List<TaskFormField> parseFields(String stored) {
        try {
            Object parsed = JSON.parse(stored);
            if (!(parsed instanceof JSONArray array)) {
                throw invalid("提交字段存储无效");
            }
            List<TaskFormField> fields = new ArrayList<>(array.size());
            for (Object entry : array) {
                if (!(entry instanceof JSONObject field) || !field.keySet().equals(FIELD_KEYS)
                        || !(field.get("key") instanceof String key)
                        || !(field.get("label") instanceof String label)
                        || !(field.get("type") instanceof String type)
                        || !(field.get("required") instanceof Boolean required)
                        || !(field.get("options") instanceof JSONArray options)) {
                    throw invalid("提交字段存储无效");
                }
                List<String> strings = new ArrayList<>(options.size());
                for (Object option : options) {
                    if (!(option instanceof String value)) {
                        throw invalid("选项定义无效");
                    }
                    strings.add(value);
                }
                fields.add(new TaskFormField(key, label, type, required, strings));
            }
            return validateFields(fields);
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("提交字段存储无效");
        }
    }

    public ValidatedValues validateValues(List<TaskFormField> fields, Map<String, Object> values) {
        if (values == null) {
            throw invalid("提交内容不能为空");
        }
        Set<String> keys = new HashSet<>();
        fields.forEach(field -> keys.add(field.key()));
        if (!keys.containsAll(values.keySet())) {
            throw invalid("提交包含未定义字段");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        List<Long> resourceIds = new ArrayList<>();
        for (TaskFormField field : fields) {
            Object value = values.get(field.key());
            boolean empty = value == null || value instanceof String text && text.isEmpty();
            if (empty) {
                if (field.required()) {
                    throw invalid(field.label() + "不能为空");
                }
                normalized.put(field.key(), null);
                continue;
            }
            if (field.type().equals("file")) {
                if (!(value instanceof Map<?, ?> file) || !file.keySet().equals(Set.of("fileId"))
                        || !(file.get("fileId") instanceof String fileId)) {
                    throw invalid(field.label() + "必须为文件引用");
                }
                long id = parseFileId(fileId);
                normalized.put(field.key(), Map.of("fileId", Long.toString(id)));
                resourceIds.add(id);
            } else {
                if (!(value instanceof String text)) {
                    throw invalid(field.label() + "必须为文本");
                }
                if (field.required() && text.isBlank()) {
                    throw invalid(field.label() + "不能为空");
                }
                if (field.type().equals("select") && !field.options().contains(text)) {
                    throw invalid(field.label() + "选项无效");
                }
                if (field.type().equals("number")) {
                    try {
                        double finite = Double.parseDouble(text);
                        if (!Double.isFinite(finite)) {
                            throw new NumberFormatException();
                        }
                        text = new BigDecimal(text).stripTrailingZeros().toPlainString();
                    } catch (NumberFormatException exception) {
                        throw invalid(field.label() + "必须为有限数字");
                    }
                }
                normalized.put(field.key(), text);
            }
        }
        return new ValidatedValues(normalized, resourceIds.stream().distinct().toList());
    }

    public ValidatedValues parseValues(List<TaskFormField> fields, String stored) {
        try {
            Object parsed = JSON.parse(stored);
            if (!(parsed instanceof JSONObject values)) {
                throw invalid("提交内容存储无效");
            }
            return validateValues(fields, values);
        } catch (AppException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw invalid("提交内容存储无效");
        }
    }

    public long parseFileId(String fileId) {
        try {
            if (!fileId.matches("[1-9][0-9]*")) {
                throw new NumberFormatException();
            }
            return Long.parseLong(fileId);
        } catch (NumberFormatException exception) {
            throw invalid("文件ID无效");
        }
    }

    private AppException invalid(String message) {
        return new AppException(ResultCode.PARAM_ERROR, message);
    }

    public record ValidatedValues(Map<String, Object> values, List<Long> resourceIds) {
        public String serialize() {
            return JSON.toJSONString(values);
        }
    }
}

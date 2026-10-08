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
import java.util.regex.Pattern;

/** The task HTTP boundary and stored snapshots share one strict form contract. */
@Component
public class TaskFormService {
    private static final Set<String> TYPES = Set.of("text", "textarea", "file", "number", "select");
    private static final Set<String> FIELD_KEYS = Set.of("key", "label", "type", "required", "options");
    private static final Pattern FILE_ID = Pattern.compile("[1-9][0-9]*");

    /** Validates unique field keys and the supported form controls before taking a snapshot. */
    public List<TaskFormField> validateFields(List<TaskFormField> fields) {
        Set<String> keys = new HashSet<>();
        for (TaskFormField field : fields) {
            validateField(field);
            if (!keys.add(field.key())) {
                throw invalid("提交字段定义无效");
            }
        }
        return fields;
    }

    private void validateField(TaskFormField field) {
        if (field == null) {
            throw invalid("提交字段定义无效");
        }
        requireFieldText(field.key());
        requireFieldText(field.label());
        if (field.type() == null || !TYPES.contains(field.type())
                || field.required() == null || field.options() == null) {
            throw invalid("提交字段定义无效");
        }
        validateOptions(field);
    }

    private void requireFieldText(String text) {
        if (text == null || text.isBlank()) {
            throw invalid("提交字段定义无效");
        }
    }

    private void validateOptions(TaskFormField field) {
        Set<String> options = new HashSet<>();
        for (String option : field.options()) {
            if (option == null || option.isBlank() || !options.add(option)) {
                throw invalid("选项定义无效");
            }
        }
        if (field.type().equals("select")) {
            if (options.isEmpty()) {
                throw invalid("选项仅用于选择字段且不能为空");
            }
        } else if (!options.isEmpty()) {
            throw invalid("选项仅用于选择字段且不能为空");
        }
    }

    /** Stores validated input definitions without exposing the storage encoding to API callers. */
    public String serializeFields(List<TaskFormField> fields) {
        return JSON.toJSONString(validateFields(fields));
    }

    /** Decodes a current-format snapshot, rejecting corruption rather than returning display fallback data. */
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

    /** Checks submitted values against the server-owned definition and collects authorized file candidates. */
    public ValidatedValues validateValues(List<TaskFormField> fields, Map<String, Object> values) {
        Set<String> keys = new HashSet<>();
        fields.forEach(field -> keys.add(field.key()));
        if (!keys.containsAll(values.keySet())) {
            throw invalid("提交包含未定义字段");
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        List<Long> resourceIds = new ArrayList<>();
        for (TaskFormField field : fields) {
            normalized.put(field.key(), normalizeValue(field, values.get(field.key()), resourceIds));
        }
        return new ValidatedValues(normalized, resourceIds.stream().distinct().toList());
    }

    private Object normalizeValue(TaskFormField field, Object value, List<Long> resourceIds) {
        boolean empty = value == null || value instanceof String text && text.isEmpty();
        if (empty) {
            if (field.required()) {
                throw invalid(field.label() + "不能为空");
            }
            return null;
        }
        return field.type().equals("file")
                ? normalizeFile(field, value, resourceIds) : normalizeText(field, value);
    }

    private Map<String, String> normalizeFile(TaskFormField field, Object value, List<Long> resourceIds) {
        if (!(value instanceof Map<?, ?> file) || file.size() != 1
                || !(file.get("fileId") instanceof String fileId)) {
            throw invalid(field.label() + "必须为文件引用");
        }
        long id = parseFileId(fileId);
        resourceIds.add(id);
        return Map.of("fileId", Long.toString(id));
    }

    private String normalizeText(TaskFormField field, Object value) {
        if (!(value instanceof String text)) {
            throw invalid(field.label() + "必须为文本");
        }
        if (field.required() && text.isBlank()) {
            throw invalid(field.label() + "不能为空");
        }
        if (field.type().equals("select") && !field.options().contains(text)) {
            throw invalid(field.label() + "选项无效");
        }
        return field.type().equals("number") ? normalizeNumber(field, text) : text;
    }

    private String normalizeNumber(TaskFormField field, String text) {
        try {
            double finite = Double.parseDouble(text);
            if (!Double.isFinite(finite)) {
                throw new NumberFormatException();
            }
            return new BigDecimal(text).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException exception) {
            throw invalid(field.label() + "必须为有限数字");
        }
    }

    /** Revalidates persisted submission values against the task's immutable field snapshot. */
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

    /** Converts a positive decimal string ID without passing through floating-point representation. */
    public long parseFileId(String fileId) {
        try {
            if (!FILE_ID.matcher(fileId).matches()) {
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

    /** Normalized submission data and distinct resource IDs for the enclosing submission transaction. */
    public record ValidatedValues(Map<String, Object> values, List<Long> resourceIds) {
        /** Persists business values only; transient file URLs and display data are not stored. */
        public String serialize() {
            return JSON.toJSONString(values);
        }
    }
}

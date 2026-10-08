package online.longlian.app.service.common;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.bo.common.TaskFormField;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskFormServiceTest {
    private final TaskFormService service = new TaskFormService();

    @Test
    void shouldRoundTripOnlyTypedSchema() {
        List<TaskFormField> fields = List.of(field("title", "text", true), field("count", "number", false));
        String stored = service.serializeFields(fields);
        assertThat(service.parseFields(stored)).isEqualTo(fields);
        assertThatThrownBy(() -> service.parseFields("[{\"name\":\"old\",\"fieldType\":\"text\"}]"))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.parseFields("{}" )).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.parseFields(null)).isInstanceOf(AppException.class);
    }

    @Test
    void shouldRejectDuplicateUnknownAndInvalidSchemaFields() {
        TaskFormField text = field("title", "text", false);
        assertThatThrownBy(() -> service.validateFields(List.of(text, text))).isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.validateFields(List.of(field("old", "group", false))))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.validateFields(List.of(new TaskFormField("", "Label", "text", false, List.of()))))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.validateFields(List.of(new TaskFormField("pick", "Label", "select", false, List.of()))))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> service.validateFields(List.of(new TaskFormField("title", "Label", "text", false, List.of("bad")))))
                .isInstanceOf(AppException.class);
    }

    @Test
    void shouldPreserveMultilineAndZeroWhileNormalizingOptionalEmptyValues() {
        List<TaskFormField> fields = List.of(field("notes", "textarea", true), field("count", "number", true),
                field("empty", "text", false), field("missing", "file", false));
        Map<String, Object> values = new HashMap<>();
        values.put("notes", " first\n  second \n");
        values.put("count", "0.00");
        values.put("empty", "");
        TaskFormService.ValidatedValues result = service.validateValues(fields, values);
        assertThat(result.values()).containsEntry("notes", " first\n  second \n").containsEntry("count", "0")
                .containsEntry("empty", null).containsEntry("missing", null);
        assertThat(result.resourceIds()).isEmpty();
        assertThat(service.parseValues(fields, result.serialize()).values()).isEqualTo(result.values());
    }

    @Test
    void shouldPreserveOptionalWhitespaceAndAcceptNonblankFieldKeys() {
        TaskFormField field = new TaskFormField("备注字段", "备注", "textarea", false, List.of());
        assertThat(service.parseFields(service.serializeFields(List.of(field)))).containsExactly(field);
        TaskFormService.ValidatedValues result = service.validateValues(List.of(field), Map.of("备注字段", " \n\t "));
        assertThat(result.values()).containsEntry("备注字段", " \n\t ");
    }

    @Test
    void shouldRejectRequiredUnknownNonStringAndNonfiniteValues() {
        List<TaskFormField> fields = List.of(field("count", "number", true));
        for (Map<String, Object> values : List.<Map<String, Object>>of(Map.of(), Map.of("count", ""),
                Map.of("count", "NaN"), Map.of("count", "Infinity"), Map.of("count", "1e999"),
                Map.of("count", 0), Map.of("count", "0", "unknown", "secret"))) {
            assertThatThrownBy(() -> service.validateValues(fields, values)).isInstanceOf(AppException.class);
        }
    }

    @Test
    void shouldEnforceSelectChoicesAndFileOnlyReferences() {
        TaskFormField choice = new TaskFormField("pick", "Choice", "select", true, List.of("A", "B"));
        assertThat(service.validateValues(List.of(choice), Map.of("pick", "A")).values()).containsEntry("pick", "A");
        assertThatThrownBy(() -> service.validateValues(List.of(choice), Map.of("pick", "C")))
                .isInstanceOf(AppException.class);
        List<TaskFormField> files = List.of(field("attachment", "file", true), field("other", "file", false));
        TaskFormService.ValidatedValues result = service.validateValues(files,
                Map.of("attachment", Map.of("fileId", "9007199254740993"), "other", Map.of("fileId", "9007199254740993")));
        assertThat(result.resourceIds()).containsExactly(9007199254740993L);
        for (Object bad : List.of("url", Map.of("fileId", 1), Map.of("fileId", "0"),
                Map.of("fileId", "9223372036854775808"), Map.of("fileId", "1", "url", "https://untrusted"))) {
            assertThatThrownBy(() -> service.validateValues(files, Map.of("attachment", bad)))
                    .isInstanceOf(AppException.class);
        }
        assertThatThrownBy(() -> service.validateValues(List.of(field("text", "text", false)),
                Map.of("text", Map.of("fileId", "1")))).isInstanceOf(AppException.class);
    }

    private TaskFormField field(String key, String type, boolean required) {
        return new TaskFormField(key, key, type, required, List.of());
    }
}

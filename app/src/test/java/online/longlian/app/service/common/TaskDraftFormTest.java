package online.longlian.app.service.common;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.pojo.bo.common.TaskFormField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskDraftFormTest {

    private final TaskFormService taskFormService = new TaskFormService();

    @Test
    void shouldAllowMissingRequiredFieldsOnlyInDrafts() {
        var fields = List.of(new TaskFormField("text", "文本", "text", true, List.of()));
        assertThat(taskFormService.validateDraftValues(fields, Map.of()).values()).containsEntry("text", null);
        assertThatThrownBy(() -> taskFormService.validateValues(fields, Map.of()))
                .isInstanceOf(AppException.class);
    }

    @Test
    void shouldStillRejectInvalidChoiceAndUnknownKeys() {
        var fields = List.of(new TaskFormField("choice", "选项", "select", true, List.of("A")));
        assertThatThrownBy(() -> taskFormService.validateDraftValues(fields, Map.of("choice", "B")))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> taskFormService.validateDraftValues(fields, Map.of("other", "A")))
                .isInstanceOf(AppException.class);
    }

    @Test
    void shouldNormalizeNumbersWithoutAcceptingJsonNumbers() {
        var fields = List.of(new TaskFormField("n", "数字", "number", true, List.of()));
        assertThat(taskFormService.validateDraftValues(fields, Map.of("n", "1.00")).values())
                .containsEntry("n", "1");
        assertThatThrownBy(() -> taskFormService.validateDraftValues(fields, Map.of("n", 1)))
                .isInstanceOf(AppException.class);
    }
}

package online.longlian.app.service.app.impl.project;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProjectCustomFieldRequirementsTest {

    @Test
    void requireSatisfied_reportsMissingNamesOnceAndInOrder() {
        assertThatThrownBy(() -> ProjectCustomFieldRequirements.requireSatisfied(
                "{\"tags\":[],\"fields\":[{\"name\":\"作者\",\"value\":\"\"},{\"name\":\"备注\",\"value\":\"有\"}]}",
                List.of("作者", "状态", "作者")))
                .isInstanceOfSatisfying(AppException.class, ex -> {
                    assertThat(ex.getCode()).isEqualTo(ResultCode.PARAM_ERROR.getCode());
                    assertThat(ex.getMessage()).isEqualTo("请填写必填字段：作者、状态");
                });
    }

    @Test
    void requireSatisfied_acceptsTrimmedNameWithNonBlankValue() {
        ProjectCustomFieldRequirements.requireSatisfied(
                "{\"tags\":[],\"fields\":[{\"name\":\" 作者 \",\"value\":\" x \"}]}",
                List.of("作者"));
    }

    @Test
    void requireSatisfied_skipsCheckWhenNothingIsRequired() {
        ProjectCustomFieldRequirements.requireSatisfied("not-json", List.of());
    }
}

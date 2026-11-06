package online.longlian.app.service.app.impl.taskdraft;

import online.longlian.app.common.constants.TaskDraftConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskDraftRevisionPolicyTest {

    @Test
    void shouldAcceptZeroOnlyForAnAbsentDraftRow() {
        TaskDraftRevisionPolicy.requireMatchingRevision(null, "0");
        assertThatThrownBy(() -> TaskDraftRevisionPolicy.requireMatchingRevision(null, "1"))
                .isInstanceOfSatisfying(AppException.class, exception -> {
                    assertThat(exception.getCode()).isEqualTo(ResultCode.OPERATION_FAIL.getCode());
                    assertThat(exception.getMsg()).isEqualTo(
                            ResultCode.OPERATION_FAIL.getMsg() + "," + TaskDraftConstants.CONFLICT_MESSAGE);
                });
    }

    @Test
    void shouldComparePersistedRevisionEvenWhenDraftContentIsEmpty() {
        TaskDraftRecordResultBO draft = TaskDraftRecordResultBO.builder().revision(4L).build();
        TaskDraftRevisionPolicy.requireMatchingRevision(draft, "4");
        assertThatThrownBy(() -> TaskDraftRevisionPolicy.requireMatchingRevision(draft, "0"))
                .isInstanceOf(AppException.class)
                .hasMessage(TaskDraftConstants.CONFLICT_MESSAGE);
    }
}

package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 任务提交记录状态
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "task_submission", field = "status")
public enum TaskSubmissionStatus implements CodeEnum {

    SUBMITTED(1, "已提交"),
    REJECTED(2, "已打回"),
    RESET(3, "已重置");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

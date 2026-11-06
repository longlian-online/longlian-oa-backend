package online.longlian.common.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import online.longlian.common.annotation.ModelEnum;

@Getter
@RequiredArgsConstructor
@ModelEnum(model = "task_draft", field = "status")
public enum TaskDraftStatus implements CodeEnum {

    ACTIVE(1, "有效"),
    CONSUMED(2, "已提交"),
    DISCARDED(3, "已废弃"),
    REPLACED(4, "已替换");

    private final Integer code;
    private final String desc;
}

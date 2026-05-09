package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "task_instance", field = "status")
public enum TaskInstanceStatus implements CodeEnum {

    PENDING(1, "待接取"),
    CLAIMED(2, "待提交"),
    COMPLETED(3, "已完成");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

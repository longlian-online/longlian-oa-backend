package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 任务实例状态
 */
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

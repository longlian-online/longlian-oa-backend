package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务实例状态
 */
@Getter
@AllArgsConstructor
public enum TaskInstanceStatus implements CodeEnum {

    PENDING(1, "待接取"),
    CLAIMED(2, "已接取"),
    COMPLETED(3, "已完成");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目列表筛选，不落库。已完成指至少有一个任务且全部任务已完成。
 */
@Getter
@AllArgsConstructor
public enum ItemStatus implements CodeEnum {

    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

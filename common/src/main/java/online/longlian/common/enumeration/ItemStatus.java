package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目状态。新建默认为进行中，由负责人标记为已完成。
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

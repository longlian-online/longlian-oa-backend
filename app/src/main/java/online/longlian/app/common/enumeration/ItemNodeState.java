package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 项目流程节点状态
 */
@Getter
@AllArgsConstructor
public enum ItemNodeState implements CodeEnum {

    COMPLETED(1, "已完成"),
    IN_PROGRESS(2, "进行中"),
    LOCKED(3, "未解锁");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

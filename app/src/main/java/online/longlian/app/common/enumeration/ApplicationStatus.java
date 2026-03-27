package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 入组申请状态
 */
@Getter
@AllArgsConstructor
public enum ApplicationStatus implements CodeEnum {

    PENDING(0, "待审核"),
    APPROVED(1, "通过"),
    REJECTED(2, "拒绝");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 入组申请状态
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "group_application", field = "status")
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

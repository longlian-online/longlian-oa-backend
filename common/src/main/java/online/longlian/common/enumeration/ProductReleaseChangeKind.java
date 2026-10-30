package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "product_release_change", field = "kind")
public enum ProductReleaseChangeKind implements CodeEnum {

    ADDED(1, "新增"),
    CHANGED(2, "调整"),
    FIXED(3, "修复");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

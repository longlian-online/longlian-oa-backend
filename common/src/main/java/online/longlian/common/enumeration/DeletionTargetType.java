package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "deletion_request", field = "target_type")
public enum DeletionTargetType implements CodeEnum {

    PROJECT(1, "企划"),
    ITEM(2, "项目");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

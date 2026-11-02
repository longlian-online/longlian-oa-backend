package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "product_release", field = "status")
public enum ProductReleaseStatus implements CodeEnum {

    DRAFT(0, "草稿"),
    PUBLISHED(1, "已发布"),
    WITHDRAWN(2, "已撤回");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

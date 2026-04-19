package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 项目状态
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "item", field = "status")
public enum ItemStatus implements CodeEnum {

    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    PUBLISHED(3, "已公布");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

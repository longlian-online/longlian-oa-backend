package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

/**
 * 幂等记录来源。开放接口是首个消费方，后续其他端接入只加枚举值，表结构不动。
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "idempotency_record", field = "source")
public enum IdempotencySource implements CodeEnum {
    OPEN_API(1, "开放接口");

    private final Integer code;
    private final String desc;
}

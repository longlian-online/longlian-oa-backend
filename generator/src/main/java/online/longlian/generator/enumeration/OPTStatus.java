package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "one_time_password", field = "status")
public enum OPTStatus implements CodeEnum{

    // 状态 0-待使用 1-已使用
    PENDING(0, "待使用"),
    USED(1, "已使用");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
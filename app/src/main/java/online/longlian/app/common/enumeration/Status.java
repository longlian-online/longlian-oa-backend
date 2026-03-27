package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Status implements CodeEnum{
    ENABLED(1,"启用"),
    DISABLED(0,"禁用");
    private final Integer code;
    private final String desc;
    @Override
    public Integer getCode() {
        return code;
    }
}

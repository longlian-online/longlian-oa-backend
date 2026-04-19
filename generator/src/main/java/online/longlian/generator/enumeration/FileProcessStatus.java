package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "resource", field = "process_status")
public enum FileProcessStatus implements CodeEnum {
    Pending(0, "待上传"),
    Activated(1, "处理中"),
    Deprecated(2, "已废弃");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
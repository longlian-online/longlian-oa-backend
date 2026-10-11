package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "resource", field = "process_status")
public enum FileProcessStatus implements CodeEnum {
    Pending(0, "待上传"),
    Activated(1, "已激活"),
    Deprecated(2, "已废弃"),
    Uploaded(3, "已上传待绑定");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

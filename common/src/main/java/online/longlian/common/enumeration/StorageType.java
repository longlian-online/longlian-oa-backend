package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "resource", field = "storage_type")
public enum StorageType implements CodeEnum {
    LOCAL(1, "本地存储"),
    OSS(2, "云对象存储");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

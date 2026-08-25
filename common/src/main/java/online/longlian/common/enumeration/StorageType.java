package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "resource", field = "storage_type")
public enum StorageType implements CodeEnum {
    NONE(0, "空实现"),
    LOCAL(1, "本地存储"),
    OSS(2, "阿里云对象存储"),
    COS(3, "腾讯云对象存储");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "file_storage", field = "storage_type")
public enum StorageType implements CodeEnum {
    LOCAL(1, "本地存储"),
    OSS(2, "阿里云OSS"),
    COS(3, "腾讯云COS");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
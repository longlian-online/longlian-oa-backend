package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
package online.longlian.app.common.enumeration;

public enum StorageType implements CodeEnum {
    LOCAL(1, "本地存储"),
    OSS(2, "阿里云OSS"),
    COS(3, "腾讯云COS");

    private final Integer code;

    StorageType(Integer code, String desc) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }
}
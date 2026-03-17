package online.longlian.app.common.enumeration;

import lombok.Getter;

@Getter
public enum FileProcessStatus implements CodeEnum {
    UN_PROCESS(0, "未处理"),
    PROCESSING(1, "处理中"),
    COMPRESSED(2, "已压缩"),
    FAILED(3, "处理失败");

    private final Integer code;

    FileProcessStatus(Integer code, String desc) {
        this.code = code;
    }

    @Override
    public Integer getCode() {
        return code;
    }
}
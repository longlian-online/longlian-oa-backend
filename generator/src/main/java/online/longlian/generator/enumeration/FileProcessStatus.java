package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "file_storage", field = "process_status")
public enum FileProcessStatus implements CodeEnum {
    UN_PROCESS(0, "未处理"),
    PROCESSING(1, "处理中"),
    COMPRESSED(2, "已压缩"),
    FAILED(3, "处理失败");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
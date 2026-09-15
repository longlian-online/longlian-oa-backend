package online.longlian.common.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;
import com.alibaba.fastjson2.annotation.JSONField;
import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "project", field = "status")
public enum ProjectStatus implements CodeEnum {
    IN_PROGRESS(1, "进行中"),
    COMPLETED(2, "已完成"),
    ARCHIVED(3, "已归档");

    private final Integer code;
    @JsonValue
    @JSONField(value = true)
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

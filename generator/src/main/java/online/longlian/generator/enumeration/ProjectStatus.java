package online.longlian.generator.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;
import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "project", field = "status")
public enum ProjectStatus implements CodeEnum {
    IN_PROGRESS(1, "进行中"),

    COMPLETED(2, "已完成"),

    ARCHIVED(3, "已归档");

    private final Integer code;
    @JsonValue
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}
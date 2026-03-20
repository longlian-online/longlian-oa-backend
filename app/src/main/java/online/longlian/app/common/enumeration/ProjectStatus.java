package online.longlian.app.common.enumeration;

import com.fasterxml.jackson.annotation.JsonValue;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
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
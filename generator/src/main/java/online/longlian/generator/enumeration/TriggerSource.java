package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "scheduled_task_log", field = "trigger_source")
public enum TriggerSource implements CodeEnum {

    SCHEDULED(1, "定时触发"),
    MANUAL(2, "手动触发");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

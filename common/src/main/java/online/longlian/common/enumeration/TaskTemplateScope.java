package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "task_template", field = "scope")
public enum TaskTemplateScope implements CodeEnum {

    PERSONAL(1, "个人模板"),
    ORGANIZATION(2, "组织通用模板");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

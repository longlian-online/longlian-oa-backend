package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 任务模板作用域
 */
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

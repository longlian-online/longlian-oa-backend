package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 任务模板作用域
 */
@Getter
@AllArgsConstructor
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

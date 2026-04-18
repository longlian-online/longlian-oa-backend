package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "permission", field = "status")
@ModelEnum(model = "role", field = "status")
@ModelEnum(model = "user", field = "status")
@ModelEnum(model = "organization", field = "status")
@ModelEnum(model = "organization_member", field = "status")
@ModelEnum(model = "project_type", field = "status")
@ModelEnum(model = "base_task", field = "status")
@ModelEnum(model = "task_template", field = "status")
public enum Status implements CodeEnum{
    ENABLED(1,"启用"),
    DISABLED(0,"禁用");
    private final Integer code;
    private final String desc;
    @Override
    public Integer getCode() {
        return code;
    }
}

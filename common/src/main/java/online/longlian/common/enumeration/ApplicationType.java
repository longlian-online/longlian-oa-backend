package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "group_application", field = "application_type")
public enum ApplicationType implements CodeEnum {

    REGISTER(0, "注册入组"),
    EXISTING_USER(1, "已注册用户入组");

    private final Integer code;
    private final String desc;
}

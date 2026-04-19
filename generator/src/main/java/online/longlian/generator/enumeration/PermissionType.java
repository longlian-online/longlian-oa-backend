package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

/**
 * 权限类型
 */
@Getter
@AllArgsConstructor
@ModelEnum(model = "permission", field = "perm_type")
public enum PermissionType implements CodeEnum {

    MENU(1, "菜单"),
    BUTTON(2, "按钮"),
    API(3, "接口");

    private final Integer code;
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

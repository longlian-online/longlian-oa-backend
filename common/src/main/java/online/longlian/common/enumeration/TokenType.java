package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "token_blacklist", field = "token_type")
public enum TokenType implements CodeEnum {

    User(1, "用户端"),
    Admin(2, "管理端");

    private final Integer code;
    private final String desc;
}

package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@AllArgsConstructor
@ModelEnum(model = "email_verify_otp", field = "business_type")
public enum EmailVerifyBusinessType implements CodeEnum {

    LOGIN(0, "登录"),
    REGISTER(1, "注册"),
    FORGOT_PASSWORD(2, "忘记密码");

    private final Integer code;
    @Getter
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

package online.longlian.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.common.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "email_verify_otp", field = "send_status")
public enum EmailVerifySendStatus implements CodeEnum {

    PENDING(0, "待发送"),
    SENT(1, "发送成功"),
    FAILED(2, "发送失败");

    private final Integer code;
    @Getter
    private final String desc;

    @Override
    public Integer getCode() {
        return code;
    }
}

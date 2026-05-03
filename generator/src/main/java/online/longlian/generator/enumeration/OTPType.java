package online.longlian.generator.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;
import online.longlian.generator.annotation.ModelEnum;

@Getter
@AllArgsConstructor
@ModelEnum(model = "one_time_password", field = "biz_type")
public enum OTPType implements CodeEnum {

    OrganizationInvite(1, "邀请创建组织"),
    OrganizationUserInvite(2, "邀请加入组织"),
    EmailVerify(3, "邮箱验证码");

    private final Integer code;
    private final String desc;

}

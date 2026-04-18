package online.longlian.app.common.enumeration;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OTPType implements CodeEnum {

    OrganizationInvite(1, "邀请创建组织"),
    OrganizationUserInvite(2, "邀请组员");

    private final Integer code;
    private final String desc;
}

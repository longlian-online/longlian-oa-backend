package online.longlian.app.common.enumeration;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

@Getter
@RequiredArgsConstructor
public enum AdminRole {
    ROOT("root"), NORMAL("normal");

    private final String value;

    public static AdminRole fromValue(String value) {
        for (AdminRole role : values()) {
            if (role.value.equals(value)) return role;
        }
        throw new AppException(ResultCode.UNAUTHORIZED, "平台角色无效");
    }
}

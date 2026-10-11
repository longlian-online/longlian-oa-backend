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
        return switch (value) {
            case "root" -> ROOT;
            case "normal" -> NORMAL;
            case null, default -> throw new AppException(ResultCode.UNAUTHORIZED, "平台角色无效");
        };
    }
}

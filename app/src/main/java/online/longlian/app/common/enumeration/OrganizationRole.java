package online.longlian.app.common.enumeration;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

public enum OrganizationRole {
    ORG_OWNER,
    ORG_ADMIN,
    ORG_USER;

    public static OrganizationRole fromValue(String value) {
        return switch (value) {
            case "ORG_OWNER" -> ORG_OWNER;
            case "ORG_ADMIN" -> ORG_ADMIN;
            case "ORG_USER" -> ORG_USER;
            case null, default -> throw new AppException(ResultCode.OPERATION_FAIL, "组织角色无效");
        };
    }
}

package online.longlian.app.common.enumeration;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

public enum OrganizationRole {
    ORG_ADMIN, ORG_USER;

    public static OrganizationRole fromValue(String value) {
        for (OrganizationRole role : values()) {
            if (role.name().equals(value)) return role;
        }
        throw new AppException(ResultCode.OPERATION_FAIL, "组织角色无效");
    }
}

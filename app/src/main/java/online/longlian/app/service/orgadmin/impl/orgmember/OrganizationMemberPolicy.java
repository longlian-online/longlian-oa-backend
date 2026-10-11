package online.longlian.app.service.orgadmin.impl.orgmember;

import online.longlian.app.common.enumeration.OrganizationRole;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.entity.OrganizationMember;
import org.springframework.stereotype.Component;

@Component
public class OrganizationMemberPolicy {
    public void requireManageableTarget(OrganizationMember operator, OrganizationMember target) {
        if (operator.getUserId().equals(target.getUserId())
                || OrganizationRole.fromValue(target.getOrgRole()) == OrganizationRole.ORG_OWNER)
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "不能操作自己或组织所有者");
        OrganizationRole role = OrganizationRole.fromValue(operator.getOrgRole());
        if (role == OrganizationRole.ORG_USER || (role == OrganizationRole.ORG_ADMIN
                && OrganizationRole.fromValue(target.getOrgRole()) != OrganizationRole.ORG_USER))
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "普通管理员只能管理普通成员");
    }

    public void requireRoleChange(OrganizationMember operator, OrganizationMember target) {
        requireManageableTarget(operator, target);
        if (OrganizationRole.fromValue(operator.getOrgRole()) != OrganizationRole.ORG_OWNER)
            throw new AppException(ResultCode.UNAUTHORIZED_OPERATION, "只有组织所有者可以调整角色");
    }
}

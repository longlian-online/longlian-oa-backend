package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.orgadmin.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminUpdateOrganizationInfoParamsBO;

/**
 * 组织管理端组织信息设置服务接口。
 * <p>
 * 提供组织管理员视角的组织信息查看和编辑功能，
 * 包括组织名称、头像、描述等基本信息的维护。
 */
public interface OrgAdminOrganizationService {

    /**
     * 获取当前组织的详细信息。
     *
     * @param orgId 组织 ID
     * @return 包含名称、头像（含访问 URL）、描述等字段的组织信息
     */
    OrgAdminGetOrganizationInfoResultBO getOrganizationInfo(Long orgId);

    /**
     * 更新组织基本信息（名称、头像、描述）。
     *
     * @param params 包含组织 ID 及待更新字段的参数
     */
    void updateOrganizationInfo(OrgAdminUpdateOrganizationInfoParamsBO params);
}

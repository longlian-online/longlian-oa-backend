package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.OrgAdminUpdateOrganizationInfoParamsBO;

public interface OrgAdminOrganizationService {

    OrgAdminGetOrganizationInfoResultBO getOrganizationInfo(Long orgId);

    void updateOrganizationInfo(OrgAdminUpdateOrganizationInfoParamsBO params);
}

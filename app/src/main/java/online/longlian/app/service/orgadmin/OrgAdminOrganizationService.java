package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.orgadmin.OrgAdminGetOrganizationInfoResultBO;
import online.longlian.app.pojo.bo.orgadmin.OrgAdminUpdateOrganizationInfoParamsBO;

public interface OrgAdminOrganizationService {

    OrgAdminGetOrganizationInfoResultBO getOrganizationInfo(Long orgId);

    void updateOrganizationInfo(OrgAdminUpdateOrganizationInfoParamsBO params);
}

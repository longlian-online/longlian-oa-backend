package online.longlian.app.service.admin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.admin.AdminGenerateCreateOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.admin.AdminGenerateInviteCodeResultBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationListResultBO;
import online.longlian.app.pojo.bo.admin.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

public interface OrganizationService {
     PageResultBO<AdminOrganizationListResultBO> getOrgListInfo(@NonNull AdminOrganizationListParamsBO params);

    AdminGenerateInviteCodeResultBO generateCreateOrgInviteCode(@NonNull AdminGenerateCreateOrgInviteCodeParamsBO params);

    void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params);
}

package online.longlian.app.service.admin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.AdminGenerateCreateOrgInviteCodeParamsBO;
import online.longlian.app.pojo.bo.AdminGenerateInviteCodeResultBO;
import online.longlian.app.pojo.bo.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.AdminOrganizationListResultBO;
import online.longlian.app.pojo.bo.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;

public interface OrganizationService {
     PageResultBO<AdminOrganizationListResultBO> getOrgListInfo(@NonNull AdminOrganizationListParamsBO params);

    AdminGenerateInviteCodeResultBO generateCreateOrgInviteCode(@NonNull AdminGenerateCreateOrgInviteCodeParamsBO params);

    void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params);
}

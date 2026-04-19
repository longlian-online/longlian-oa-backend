package online.longlian.app.service.admin;

import lombok.NonNull;
import online.longlian.app.pojo.bo.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.AdminOrganizationListResultBO;
import online.longlian.app.pojo.bo.AdminOrganizationUpdateStatusParamsBO;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.vo.orgadmin.InviteCodeVO;

public interface OrganizationService {
     PageResultBO<AdminOrganizationListResultBO> getOrgListInfo(@NonNull AdminOrganizationListParamsBO params);
    InviteCodeVO generateCreateOrgInviteCode();
    void updateOrgStatus(@NonNull AdminOrganizationUpdateStatusParamsBO params);
}

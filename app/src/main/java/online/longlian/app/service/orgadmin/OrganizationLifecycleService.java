package online.longlian.app.service.orgadmin;
import online.longlian.app.pojo.bo.orgadmin.*;
public interface OrganizationLifecycleService {
    void removeMember(OrgMemberRemoveParamsBO params);
    void exitOrganization(OrgMemberExitParamsBO params);
    void transferOwnership(OrgMemberTransferOwnershipParamsBO params);
    void dissolveOrganization(OrgDissolveParamsBO params);
}

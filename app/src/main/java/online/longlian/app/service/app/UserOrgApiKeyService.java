package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.UserOrgApiKeyParamsBO;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyResultBO;
import online.longlian.app.pojo.bo.app.UserOrgApiKeyGenerateResultBO;
import online.longlian.app.pojo.bo.common.McpApiKeyIdentityResultBO;
import online.longlian.app.pojo.bo.common.OrganizationMembershipOrganizationResultBO;

import java.util.List;

public interface UserOrgApiKeyService {

    List<OrganizationMembershipOrganizationResultBO> listOwnOrganizations(Long userId);

    UserOrgApiKeyResultBO getCurrent(UserOrgApiKeyParamsBO params);

    UserOrgApiKeyGenerateResultBO generate(UserOrgApiKeyParamsBO params);

    void revoke(UserOrgApiKeyParamsBO params);

    /** 参与密码变更等调用方事务，永久撤销本人全部组织凭据。 */
    void revokeAllForUser(Long userId);

    /** 成员退出或移除流程在其事务内调用，重新加入不能恢复旧凭据。 */
    void revokeForMembership(UserOrgApiKeyParamsBO params);

    McpApiKeyIdentityResultBO authenticate(String apiKey);
}

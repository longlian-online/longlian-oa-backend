package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.common.CurrentOrganizationContextBO;

/**
 * Resolves organization membership from MySQL. Session selection is stored with the JWT session,
 * not in a second user-scoped cache.
 */
public interface CurrentOrganizationService {

    Long resolveCurrentOrgId(Long userId);

    CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId);

    CurrentOrganizationContextBO switchCurrentOrg(Long userId, Long targetOrgId);
}

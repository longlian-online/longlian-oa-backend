package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.CurrentOrganizationContextBO;

public interface CurrentOrganizationService {

    /**
     * 解析用户当前可用组织ID。
     */
    Long resolveCurrentOrgId(Long userId);

    /**
     * 解析用户当前组织上下文。
     */
    CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId);

    /**
     * 刷新用户当前组织上下文缓存过期时间。
     */
    void refreshCurrentOrgTtl(Long userId, long ttlSeconds);

    /**
     * 切换用户当前组织。
     */
    void switchCurrentOrg(Long userId, Long targetOrgId);

    /**
     * 清除用户当前组织上下文。
     */
    void clearCurrentOrg(Long userId);
}

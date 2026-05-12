package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.CurrentOrganizationContextBO;

public interface CurrentOrganizationService {

    /**
     * 解析用户当前可用组织ID。
     */
    Long resolveCurrentOrgId(Long userId);

    /**
     * 解析用户当前组织上下文，允许在当前组织不可用时回退到默认组织或第一个可用组织。
     * 仅适用于登录初始化等需要自动选择组织的场景。
     */
    CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId);

    /**
     * 获取业务请求使用的当前组织ID，不自动回退到其他组织。
     */
    Long requireCurrentOrgId(Long userId);

    /**
     * 获取业务请求必须使用的当前组织上下文，不自动回退到其他组织。
     */
    CurrentOrganizationContextBO requireCurrentOrgContext(Long userId);

    /**
     * 刷新用户当前组织上下文缓存过期时间。
     */
    void refreshCurrentOrgTtl(Long userId, Long currentOrgId, long ttlSeconds);

    /**
     * 切换用户当前组织。
     */
    void switchCurrentOrg(Long userId, Long targetOrgId);

    /**
     * 清除用户当前组织上下文。
     */
    void clearCurrentOrg(Long userId);
}

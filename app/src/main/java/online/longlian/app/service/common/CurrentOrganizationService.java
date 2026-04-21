package online.longlian.app.service.common;

public interface CurrentOrganizationService {

    /**
     * 解析用户当前可用组织ID。
     */
    Long resolveCurrentOrgId(Long userId);

    /**
     * 初始化用户当前组织上下文。
     */
    void initializeCurrentOrg(Long userId);

    /**
     * 切换用户当前组织。
     */
    void switchCurrentOrg(Long userId, Long targetOrgId);

    /**
     * 清除用户当前组织上下文。
     */
    void clearCurrentOrg(Long userId);
}

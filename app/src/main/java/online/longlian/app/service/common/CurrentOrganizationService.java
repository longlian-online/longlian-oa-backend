package online.longlian.app.service.common;

import online.longlian.app.pojo.bo.common.CurrentOrganizationContextBO;

/**
 * 当前组织上下文管理服务接口。
 * <p>
 * 负责用户当前活跃组织的记忆、校验和切换：
 * <ol>
 *   <li><b>解析（resolve）</b>：登录初始化时自动选择可用组织，
 *       优先使用 Redis 缓存的最近组织 → 回退到用户的默认组织 → 遍历用户所有有效组织取第一个</li>
 *   <li><b>获取（require）</b>：业务请求中获取当前组织，不自动回退，
 *       缓存不存在或组织不可用时报错</li>
 *   <li><b>切换（switch）</b>：主动切换到指定组织并刷新缓存</li>
 * </ol>
 * <p>
 * 当前组织 ID 以 Redis 缓存为主，缓存过期时间对齐用户登录会话的 TTL。
 */
public interface CurrentOrganizationService {

    /**
     * 解析用户当前可用组织 ID（带自动回退逻辑）。
     * <p>
     * 缓存命中且组织有效时直接返回；否则按默认组织 → 最先加入的有效组织 的顺序回退。
     *
     * @param userId 用户 ID
     * @return 可用的组织 ID，无可用组织时抛出异常
     */
    Long resolveCurrentOrgId(Long userId);

    /**
     * 解析用户当前组织上下文，包含组织 ID、成员 ID 和角色列表。
     * <p>
     * 允许在当前组织不可用时回退到默认组织或第一个可用组织，
     * 仅适用于登录初始化等需要自动选择组织的场景。
     *
     * @param userId       用户 ID
     * @param defaultOrgId 默认组织 ID（可为 null）
     * @return 包含组织 ID、成员 ID、角色列表的上下文对象
     */
    CurrentOrganizationContextBO resolveCurrentOrgContext(Long userId, Long defaultOrgId);

    /**
     * 获取业务请求使用的当前组织 ID，不自动回退到其他组织。
     * <p>
     * 缓存不存在或组织不可用时直接抛出异常，适用于需要明确组织上下文的业务场景。
     *
     * @param userId 用户 ID
     * @return 当前组织 ID
     */
    Long requireCurrentOrgId(Long userId);

    /**
     * 获取业务请求必须使用的当前组织上下文，不自动回退到其他组织。
     *
     * @param userId 用户 ID
     * @return 包含组织 ID、成员 ID、角色列表的上下文对象
     */
    CurrentOrganizationContextBO requireCurrentOrgContext(Long userId);

    /**
     * 刷新用户当前组织上下文的 Redis 缓存过期时间。
     *
     * @param userId        用户 ID
     * @param currentOrgId  当前组织 ID
     * @param ttlSeconds    缓存过期秒数（通常对齐登录 Token 的剩余有效时间）
     */
    void refreshCurrentOrgTtl(Long userId, Long currentOrgId, long ttlSeconds);

    /**
     * 切换用户当前活跃组织，校验用户是否为该组织有效成员后刷新缓存。
     *
     * @param userId       用户 ID
     * @param targetOrgId  目标组织 ID
     */
    void switchCurrentOrg(Long userId, Long targetOrgId);

    /**
     * 清除用户当前组织上下文的 Redis 缓存。
     *
     * @param userId 用户 ID
     */
    void clearCurrentOrg(Long userId);
}

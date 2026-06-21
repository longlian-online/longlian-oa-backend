package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminLoginParamsBO;
import online.longlian.app.pojo.bo.admin.AdminLoginResultBO;
import online.longlian.app.pojo.bo.admin.AdminLogoutParamsBO;

/**
 * 管理端会话服务接口。
 * <p>
 * 负责管理员的登录认证和登出管理，与用户端会话服务平行但独立：
 * <ol>
 *   <li>登录时校验用户名密码，签发类型为 {@code Admin} 的 JWT Token 并更新最后登录时间</li>
 *   <li>登出时通过 {@link online.longlian.app.service.TokenBlacklistService} 将 Token 加入黑名单</li>
 *   <li>提供从 SecurityContext 获取当前管理员 ID 的便捷方法</li>
 * </ol>
 */
public interface AdminSessionService {

    /**
     * 管理员密码登录。
     *
     * @param params 包含用户名和明文的登录参数
     * @return 包含 JWT Token、管理员 ID、用户名及角色的登录结果
     */
    AdminLoginResultBO login(AdminLoginParamsBO params);

    /**
     * 管理员登出，将 Token 加入黑名单。
     *
     * @param params 包含待吊销 Token 及管理员 ID 的登出参数
     */
    void logout(AdminLogoutParamsBO params);

    /**
     * 从 SecurityContext 获取当前登录管理员 ID，未认证时返回 null。
     *
     * @return 当前管理员 ID，未登录时返回 null
     */
    Long getCurrentAdminId();
}

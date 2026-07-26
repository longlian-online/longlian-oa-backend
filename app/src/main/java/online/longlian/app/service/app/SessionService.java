package online.longlian.app.service.app;

import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;

import java.util.List;

/**
 * 用户会话服务接口。
 * <p>
 * 负责用户登录认证、会话维持及登出管理：
 * <ol>
 *   <li>支持密码登录和邮箱验证码登录两种方式，认证成功后签发 JWT Token 并缓存会话至 Redis</li>
 *   <li>登出时将 Token 加入 {@link online.longlian.app.service.TokenBlacklistService}
 *       黑名单并清除 Redis 会话缓存</li>
 *   <li>提供当前用户信息获取和当前组织刷新的便捷方法，供 Controller 层获取会话上下文后传入其他 Service</li>
 * </ol>
 */
public interface SessionService {

    /**
     * 密码登录。
     *
     * @param params 包含用户名/邮箱和明文的登录参数
     * @return 包含 JWT Token、用户基本信息及当前组织信息的登录结果
     */
    SessionLoginResultBO loginByPwd(SessionLoginByPwdParamsBO params);

    /**
     * 邮箱验证码登录。
     *
     * @param params 包含邮箱和验证码的登录参数
     * @return 包含 JWT Token、用户基本信息及当前组织信息的登录结果
     */
    SessionLoginResultBO loginByCode(SessionLoginByCodeParamsBO params);

    /**
     * 登出。
     * <p>
     * 将 Token 加入黑名单、清除 Redis 登录状态缓存并清空当前组织上下文。
     *
     * @param params 包含待吊销 Token 及用户 ID 的登出参数
     */
    void logout(SessionLogoutParamsBO params);

    /**
     * 刷新当前用户在 SecurityContext 和 Redis 中的组织信息。
     *
     * @param currentOrgId 切换后的组织 ID
     * @param roles        切换后的角色列表
     */
    void refreshCurrentUserOrg(Long currentOrgId, List<String> roles);

    /**
     * 从 SecurityContext 获取当前登录用户详情，未认证或 Token 无效时抛出
     * {@link online.longlian.app.common.result.ResultCode#UNAUTHORIZED} 异常。
     *
     * @return 当前登录用户的认证详情
     */
    UserDetailImpl getCurrentUser();

    /**
     * 获取当前登录用户 ID。
     *
     * @return 当前登录用户的 ID
     */
    Long getCurrentUserId();

    /**
     * 清除用户会话缓存。
     * <p>
     * 当用户被禁用时调用，确保下一次请求该用户的 Token 将因缓存未命中而触发
     * {@link online.longlian.app.common.security.UserDetailsServiceImpl#loadUserById} 重新从 DB 加载，
     * 从而校验到最新的禁用状态。
     *
     * @param userId 用户 ID
     */
    void clearUserSessionCache(Long userId);
}

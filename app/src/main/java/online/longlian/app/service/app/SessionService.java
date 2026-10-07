package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;


/**
 * 用户会话服务接口。
 * <p>
 * 负责用户登录认证、会话维持及登出管理：
 * <ol>
 *   <li>支持密码登录和邮箱验证码登录两种方式，认证成功后签发 JWT Token 并缓存用户身份至 Redis</li>
 *   <li>登出时将 Token 加入 {@link online.longlian.app.service.TokenBlacklistService}
 *       黑名单并清除 Redis 登录缓存</li>
 *   <li>登录结果里的组织只是建议打开的组织，不参与后续请求鉴权</li>
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
     * 将 Token 加入黑名单并清除 Redis 登录缓存。
     *
     * @param params 包含待吊销 Token 及用户 ID 的登出参数
     */
    void logout(SessionLogoutParamsBO params);

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

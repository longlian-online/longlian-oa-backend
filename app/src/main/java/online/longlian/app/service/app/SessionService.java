package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;
import online.longlian.app.pojo.bo.app.SessionRefreshResultBO;


/**
 * 用户会话服务接口。
 * <p>
 * 登录签发短期访问凭证和刷新凭证。刷新凭证轮换；登出只结束当前设备。
 * 密码变更作废该用户全部刷新会话，并让此前的访问凭证失效。
 * 登录结果里的组织是用户保存的默认组织，不参与后续请求鉴权。
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
     * 登出当前设备。访问凭证进入黑名单，对应的刷新会话作废。
     * 不删除按用户共享的登录资料缓存，其他设备仍可继续使用。
     *
     * @param params 包含待吊销访问凭证及用户 ID 的登出参数
     */
    void logout(SessionLogoutParamsBO params);

    /**
     * 用刷新凭证换发一对新凭证。不改变用户当前选择的组织。
     *
     * @param refreshToken 客户端持有的刷新凭证
     * @return 新的访问凭证和刷新凭证
     */
    SessionRefreshResultBO refresh(String refreshToken);

    /**
     * 作废该用户全部刷新会话。须在访问凭证黑名单写入之前调用，避免并发刷新发出黑名单截点之后的新访问凭证。
     *
     * @param userId 用户 ID
     */
    void revokeRefreshSessions(Long userId);

    /**
     * 清除用户登录资料缓存。
     * <p>
     * 当用户被禁用或密码变更提交后调用，下一次请求因缓存未命中而从 DB 加载，从而看到最新状态。
     * 刷新会话的作废不在这里做。
     *
     * @param userId 用户 ID
     */
    void clearUserSessionCache(Long userId);
}

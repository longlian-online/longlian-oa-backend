package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;

import java.util.List;

/**
 * 用户会话服务接口。
 */
public interface SessionService {

    SessionLoginResultBO loginByPwd(SessionLoginByPwdParamsBO params);

    SessionLoginResultBO loginByCode(SessionLoginByCodeParamsBO params);

    void logout(SessionLogoutParamsBO params);

    /**
     * 仅更新当前 JWT 会话的组织上下文，组织和角色永远写在同一缓存记录中。
     */
    void refreshCurrentUserOrg(String sessionId, Long userId, Long currentOrgId, List<String> roles);

    /**
     * 立即吊销指定用户的所有旧令牌，使权限变更不会保留在任何设备会话中。
     */
    void clearUserSessionCache(Long userId);
}

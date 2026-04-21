package online.longlian.app.service.user;

import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.bo.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.SessionLoginResultBO;
import online.longlian.app.pojo.bo.SessionLogoutParamsBO;

public interface SessionService {

    SessionLoginResultBO loginByPwd(SessionLoginByPwdParamsBO params);

    SessionLoginResultBO loginByCode(SessionLoginByCodeParamsBO params);

    void logout(SessionLogoutParamsBO params);

    UserDetailImpl getCurrentUser();

    Long getCurrentUserId();
}

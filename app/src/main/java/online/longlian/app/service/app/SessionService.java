package online.longlian.app.service.app;

import online.longlian.app.common.security.UserDetailImpl;
import online.longlian.app.pojo.bo.app.SessionLoginByCodeParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginByPwdParamsBO;
import online.longlian.app.pojo.bo.app.SessionLoginResultBO;
import online.longlian.app.pojo.bo.app.SessionLogoutParamsBO;

import java.util.List;

public interface SessionService {

    SessionLoginResultBO loginByPwd(SessionLoginByPwdParamsBO params);

    SessionLoginResultBO loginByCode(SessionLoginByCodeParamsBO params);

    void logout(SessionLogoutParamsBO params);

    void refreshCurrentUserOrg(Long currentOrgId, List<String> roles);

    UserDetailImpl getCurrentUser();

    Long getCurrentUserId();
}

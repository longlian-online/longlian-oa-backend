package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.AdminLoginParamsBO;
import online.longlian.app.pojo.bo.AdminLoginResultBO;
import online.longlian.app.pojo.bo.AdminLogoutParamsBO;

public interface SessionService {

    AdminLoginResultBO login(AdminLoginParamsBO params);

    void logout(AdminLogoutParamsBO params);

    Long getCurrentAdminId();
}

package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminLoginParamsBO;
import online.longlian.app.pojo.bo.admin.AdminLoginResultBO;
import online.longlian.app.pojo.bo.admin.AdminLogoutParamsBO;

public interface AdminSessionService {

    AdminLoginResultBO login(AdminLoginParamsBO params);

    void logout(AdminLogoutParamsBO params);

    Long getCurrentAdminId();
}

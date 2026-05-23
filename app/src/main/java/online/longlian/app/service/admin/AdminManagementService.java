package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminCreateParamsBO;
import online.longlian.app.pojo.bo.admin.AdminListParamsBO;
import online.longlian.app.pojo.bo.admin.AdminListResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

public interface AdminManagementService {

    Long create(AdminCreateParamsBO params, Long operatorId);

    void delete(Long id, Long operatorId);

    PageResultBO<AdminListResultBO> list(AdminListParamsBO params);

}

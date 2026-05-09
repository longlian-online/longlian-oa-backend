package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.AdminCreateParamsBO;
import online.longlian.app.pojo.bo.AdminListParamsBO;
import online.longlian.app.pojo.bo.AdminListResultBO;
import online.longlian.app.pojo.bo.PageResultBO;

public interface AdminManagementService {

    Long create(AdminCreateParamsBO params, Long operatorId);

    void delete(Long id, Long operatorId);

    PageResultBO<AdminListResultBO> list(AdminListParamsBO params);

}

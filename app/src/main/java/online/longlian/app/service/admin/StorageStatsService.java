package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.admin.StorageStatsResultBO;

public interface StorageStatsService {
    PageResultBO<StorageStatsResultBO> list(AdminOrganizationListParamsBO params);
}

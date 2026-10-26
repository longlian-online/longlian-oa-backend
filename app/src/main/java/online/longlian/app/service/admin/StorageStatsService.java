package online.longlian.app.service.admin;

import online.longlian.app.pojo.bo.admin.AdminOrganizationListParamsBO;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.vo.admin.StorageStatsVO;

public interface StorageStatsService {
    PageResultBO<StorageStatsVO> list(AdminOrganizationListParamsBO params);
}

package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;

public interface BaseTaskService {

    PageResultBO<BaseTaskListResultBO> listBaseTasks(BaseTaskListParamsBO params);

    void createBaseTask(BaseTaskCreateParamsBO params);

    void changeBaseTaskStatus(BaseTaskChangeStatusParamsBO params);
}

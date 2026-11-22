package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenMyTaskBO;
import online.longlian.app.pojo.bo.open.OpenMyTaskListParamsBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;

/**
 * 开放面我的待办查询：密钥用户已接取任务，(claimed_at, id) 键集游标。
 */
public interface OpenMyTaskService {

    OpenPageResultBO<OpenMyTaskBO> listMyTasks(OpenMyTaskListParamsBO params);
}

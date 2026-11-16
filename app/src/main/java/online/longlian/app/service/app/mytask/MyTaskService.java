package online.longlian.app.service.app.mytask;

import online.longlian.app.pojo.bo.app.mytask.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

/**
 * 「我的待办」：当前用户已接取任务，按可提交 / 等前置分组分页。
 */
public interface MyTaskService {

    PageResultBO<MyTaskResultBO> listMyTasks(MyTaskParamsBO params);
}

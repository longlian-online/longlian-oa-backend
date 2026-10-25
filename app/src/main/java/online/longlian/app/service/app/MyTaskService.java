package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.MyTaskParamsBO;
import online.longlian.app.pojo.vo.app.MyTaskVO;
import online.longlian.app.pojo.vo.common.PageResultVO;

/**
 * 「我的待办」：当前用户已接取任务，按可提交 / 等前置分组分页。
 */
public interface MyTaskService {

    PageResultVO<MyTaskVO> listMyTasks(MyTaskParamsBO params);
}

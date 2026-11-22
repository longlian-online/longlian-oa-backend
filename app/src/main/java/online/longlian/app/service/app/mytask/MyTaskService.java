package online.longlian.app.service.app.mytask;

import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskKeysetResultBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskParamsBO;
import online.longlian.app.pojo.bo.app.mytask.MyTaskResultBO;
import online.longlian.app.pojo.bo.common.PageResultBO;

/**
 * 「我的待办」：当前用户已接取任务，按可提交 / 等前置分组分页。
 */
public interface MyTaskService {

    PageResultBO<MyTaskResultBO> listMyTasks(MyTaskParamsBO params);

    /**
     * 我的待办键集分页：口径与 {@link #listMyTasks(MyTaskParamsBO)} 一致
     * （CLAIMED、可提交/等前置分组、按接取时间升序），(claimed_at, id) 复合游标，
     * bucket 为空时两个分组都返回。供开放面顺序翻页使用。
     */
    MyTaskKeysetResultBO listMyTasksKeyset(MyTaskKeysetParamsBO params);
}

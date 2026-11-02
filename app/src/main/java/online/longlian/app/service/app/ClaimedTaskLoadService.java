package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.ClaimedTaskLoadParamsBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;

import java.util.List;

/**
 * 当前组织内、指定成员名下尚未提交的任务。
 * 可提交与等前置的划分与「我的待办」相同。
 */
public interface ClaimedTaskLoadService {

    List<ClaimedTaskLoadResultBO> listClaimedTasks(ClaimedTaskLoadParamsBO params);
}

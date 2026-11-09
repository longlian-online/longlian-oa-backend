package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.ClaimedTaskLoadParamsBO;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadResultBO;
import online.longlian.app.pojo.bo.app.ProjectClaimedTaskParamsBO;

import java.util.List;

/**
 * 当前组织启用企划内、指定成员名下尚未提交的任务。
 * 可提交与等前置的划分与「我的待办」相同。
 */
public interface ClaimedTaskLoadService {

    List<ClaimedTaskLoadResultBO> listClaimedTasks(ClaimedTaskLoadParamsBO params);

    /** 退出企划前检查未完成责任；当前读参与调用方事务，避免使用退出前的旧快照。 */
    boolean hasClaimedTasksInProject(ProjectClaimedTaskParamsBO params);
}

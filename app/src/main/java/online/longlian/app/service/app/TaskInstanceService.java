package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;

import java.util.List;

public interface TaskInstanceService {

    TaskInstanceDetailVO getTaskInstanceDetail(TaskInstanceDetailParamsBO params);

    List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params);

    void claimTask(TaskInstanceOperateParamsBO params);

    void abandonTask(TaskInstanceOperateParamsBO params);

    void submitTask(TaskInstanceSubmitParamsBO params);

    void resetTask(TaskInstanceOperateParamsBO params);

    void rejectTask(TaskInstanceRejectParamsBO params);

}

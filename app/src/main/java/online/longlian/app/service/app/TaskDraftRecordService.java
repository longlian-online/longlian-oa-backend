package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetByIdParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryResultBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileBindParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePromoteParamsBO;

import java.util.Map;

/** 草稿记录的持久化与生命周期管理；写操作参与调用方的任务流事务。 */
public interface TaskDraftRecordService {

    TaskDraftRecordResultBO getTaskDraftRecord(TaskDraftRecordGetParamsBO params);

    TaskDraftRecordResultBO getTaskDraftRecordById(TaskDraftRecordGetByIdParamsBO params);

    TaskDraftRecordResultBO saveTaskDraftRecord(TaskDraftRecordSaveParamsBO params);

    void consumeTaskDraftRecord(TaskDraftConsumeParamsBO params);

    void discardTaskDrafts(TaskDraftDiscardParamsBO params);

    /** 删除任务实例时，同时弃置这些实例的草稿和正式提交附件。放弃接取只弃置草稿，不走这里。 */
    void abandonTaskInstanceResources(TaskDraftDiscardParamsBO params);

    Map<Long, TaskDraftSummaryResultBO> getTaskDraftSummaries(TaskDraftSummaryParamsBO params);

    /** 把本次暂存引用的上传绑到新草稿。未绑定上传被激活；上一份草稿的附件只改绑。 */
    void bindTaskDraftFiles(TaskDraftFileBindParamsBO params);

    /** 正式提交时，把指定草稿的附件改绑为任务提交附件。 */
    void promoteTaskDraftFiles(TaskDraftFilePromoteParamsBO params);
}

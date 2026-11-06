package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryResultBO;

import java.util.Map;

/** 草稿记录的持久化与生命周期管理；写操作参与调用方的任务流事务。 */
public interface TaskDraftRecordService {

    TaskDraftRecordResultBO getTaskDraftRecord(TaskDraftRecordGetParamsBO params);

    TaskDraftRecordResultBO saveTaskDraftRecord(TaskDraftRecordSaveParamsBO params);

    void consumeTaskDraftRecord(TaskDraftConsumeParamsBO params);

    void discardTaskDrafts(TaskDraftDiscardParamsBO params);

    Map<Long, TaskDraftSummaryResultBO> getTaskDraftSummaries(TaskDraftSummaryParamsBO params);
}

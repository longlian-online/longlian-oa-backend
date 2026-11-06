package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.common.TaskDraftFileCancelParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileCommitParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileListParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePrepareParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePrepareResultBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileResultBO;

import java.util.Map;

/** 草稿附件的快照准备、事务内绑定及读取。 */
public interface TaskDraftResourceService {

    TaskDraftFilePrepareResultBO prepareTaskDraftFiles(TaskDraftFilePrepareParamsBO params);

    void cancelTaskDraftFilePreparation(TaskDraftFileCancelParamsBO params);

    void commitTaskDraftFiles(TaskDraftFileCommitParamsBO params);

    Map<String, TaskDraftFileResultBO> getTaskDraftFiles(TaskDraftFileListParamsBO params);
}

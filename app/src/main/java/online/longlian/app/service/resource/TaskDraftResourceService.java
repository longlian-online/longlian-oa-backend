package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.common.TaskDraftFileListParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileResultBO;

import java.util.Map;

/** 读取已绑定到草稿的附件。绑定本身走资源服务的通用激活。 */
public interface TaskDraftResourceService {

    Map<String, TaskDraftFileResultBO> getTaskDraftFiles(TaskDraftFileListParamsBO params);
}

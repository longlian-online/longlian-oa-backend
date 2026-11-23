package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenSubmissionVersionBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailBO;
import online.longlian.app.pojo.bo.open.OpenTaskDetailParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmissionsParamsBO;

import java.util.List;

/**
 * 开放面任务实例查询：详情与提交版本列表。
 * 提交字段、附件展示与归属校验复用站内任务域组装；开放面只做 VO 收窄与标识补全。
 */
public interface OpenTaskQueryService {

    OpenTaskDetailBO getTaskDetail(OpenTaskDetailParamsBO params);

    List<OpenSubmissionVersionBO> listSubmissions(OpenTaskSubmissionsParamsBO params);
}

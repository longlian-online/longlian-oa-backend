package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskWebhookFailureResultBO;

public interface TaskWebhookFailureService {
    PageResultBO<TaskWebhookFailureResultBO> listFailures(Long orgId, int pageNum, int pageSize);

    void replay(Long orgId, Long messageId);
}

package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;

public interface TaskTemplateService {

    PageResultBO<TaskTemplateListResultBO> listTaskTemplates(TaskTemplateListParamsBO params);

    TaskTemplateDetailResultBO getTaskTemplateDetail(Long templateId, Long orgId);

    void createTaskTemplate(TaskTemplateCreateParamsBO params);

    void updateTaskTemplate(TaskTemplateUpdateParamsBO params);

    void changeTaskTemplateStatus(TaskTemplateChangeStatusParamsBO params);
}

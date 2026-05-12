package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;

public interface ProjectTypeService {

    PageResultBO<ProjectTypeListResultBO> listProjectTypes(ProjectTypeListParamsBO params);

    void createProjectType(ProjectTypeCreateParamsBO params);

    void changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO params);
}

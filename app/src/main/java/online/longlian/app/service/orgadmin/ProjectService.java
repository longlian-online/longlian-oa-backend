package online.longlian.app.service.orgadmin;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.entity.Project;

public interface ProjectService extends IService<Project> {

    PageResultBO<ProjectAdminListResultBO> getAdminProjectList(ProjectAdminListParamsBO params);

    void changeProjectStatus(ProjectChangeStatusParamsBO params);
}

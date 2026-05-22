package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.vo.app.ProjectTypeInfoVO;

import java.util.List;

public interface ProjectService {

    PageResultBO<ProjectListResultBO> getProjectList(ProjectListParamsBO params);

    ProjectDetailResultBO getProjectDetail(Long projectId, Long userId, Long orgId);

    List<ProjectTypeInfoVO> getProjectTypes(Long orgId);

    void createProject(ProjectCreateParamsBO params);

    void updateProject(ProjectUpdateParamsBO params);

    void addToWorkshop(ProjectWorkshopAddParamsBO params);

    void removeFromWorkshop(ProjectWorkshopRemoveParamsBO params);
}

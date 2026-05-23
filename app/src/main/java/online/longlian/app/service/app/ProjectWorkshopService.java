package online.longlian.app.service.app;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.app.WorkshopTaskTemplateUpdateParamsBO;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import online.longlian.app.pojo.vo.app.WorkshopTaskTemplateVO;

public interface ProjectWorkshopService extends IService<ProjectWorkshop> {

    PageResultBO<WorkshopProjectInfoVO> getMyWorkshopList(WorkshopListParamsBO params);

    PageResultBO<WorkshopTaskTemplateVO> getWorkshopTaskTemplateList(WorkshopTaskTemplateListParamsBO params);

    void createWorkshopTaskTemplate(WorkshopTaskTemplateCreateParamsBO params);

    void updateWorkshopTaskTemplate(WorkshopTaskTemplateUpdateParamsBO params);
}

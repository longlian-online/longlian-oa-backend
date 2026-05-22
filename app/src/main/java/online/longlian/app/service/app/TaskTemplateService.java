package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskTemplateOptionsParamsBO;
import online.longlian.app.pojo.vo.app.TaskTemplateOptionVO;

import java.util.List;

public interface TaskTemplateService {

    List<TaskTemplateOptionVO> listOptions(TaskTemplateOptionsParamsBO params);
}

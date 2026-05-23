package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.PageResultBO;
import online.longlian.app.pojo.bo.app.ItemCreateParamsBO;
import online.longlian.app.pojo.bo.app.ItemListParamsBO;
import online.longlian.app.pojo.bo.app.ItemOperationParamsBO;
import online.longlian.app.pojo.vo.app.ProjectItemListVO;

public interface ItemService {

    PageResultBO<ProjectItemListVO> listProjectItems(ItemListParamsBO params);

    void createProjectItem(ItemCreateParamsBO params);

    void deleteProjectItem(ItemOperationParamsBO params);

    void publishProjectItem(ItemOperationParamsBO params);
}

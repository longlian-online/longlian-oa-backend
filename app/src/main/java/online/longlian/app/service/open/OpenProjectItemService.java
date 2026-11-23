package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemBO;
import online.longlian.app.pojo.bo.open.OpenProjectItemListParamsBO;

/**
 * 开放面企划下项目查询：(updated_at, id) 键集游标 + updatedSince 增量。
 */
public interface OpenProjectItemService {

    OpenPageResultBO<OpenProjectItemBO> listProjectItems(OpenProjectItemListParamsBO params);
}

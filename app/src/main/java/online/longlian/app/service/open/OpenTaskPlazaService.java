package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.app.taskplaza.PlazaProjectGroupResultBO;
import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskPlazaListParamsBO;

/**
 * 开放面任务广场查询：缺人任务按企划分组，企划 (updated_at, id) 键集游标。
 */
public interface OpenTaskPlazaService {

    OpenPageResultBO<PlazaProjectGroupResultBO> getTaskPlaza(OpenTaskPlazaListParamsBO params);
}

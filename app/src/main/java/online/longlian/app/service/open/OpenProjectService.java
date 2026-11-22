package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenPageResultBO;
import online.longlian.app.pojo.bo.open.OpenProjectBO;
import online.longlian.app.pojo.bo.open.OpenProjectListParamsBO;

/**
 * 开放面企划查询。
 */
public interface OpenProjectService {

    /**
     * 密钥所属组织内的企划 (updated_at, id) 键集分页，支持 updatedSince 增量。
     */
    OpenPageResultBO<OpenProjectBO> listProjects(OpenProjectListParamsBO params);
}

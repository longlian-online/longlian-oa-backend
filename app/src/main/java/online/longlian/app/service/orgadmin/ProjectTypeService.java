package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;

/**
 * 组织管理端企划类型服务接口。
 * <p>
 * 负责组织内企划分类型的管理（分类标签），用于企划创建时的分类筛选和展示。
 * 仅组织管理员可操作，创建后默认启用。
 */
public interface ProjectTypeService {

    /**
     * 分页查询组织下的企划类型列表，支持关键词搜索和状态筛选。
     *
     * @param params 包含分页参数、关键词、状态筛选及组织 ID 的查询参数
     * @return 分页的企划类型列表
     */
    PageResultBO<ProjectTypeListResultBO> listProjectTypes(ProjectTypeListParamsBO params);

    /**
     * 创建企划类型，默认状态为启用。
     *
     * @param params 包含类型名称、所属组织 ID 及创建者 ID 的创建参数
     */
    void createProjectType(ProjectTypeCreateParamsBO params);

    /**
     * 变更企划类型状态（启用/禁用）。
     * <p>
     * 仅组织管理员可操作，且仅允许操作本组织的类型。
     *
     * @param params 包含类型 ID、目标状态及组织 ID 的变更参数
     */
    void changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO params);
}

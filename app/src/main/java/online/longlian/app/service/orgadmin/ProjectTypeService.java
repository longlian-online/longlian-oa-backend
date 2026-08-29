package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeDeleteParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectTypeUpdateParamsBO;

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
     * 修改企划类型名称。
     * <p>
     * 修改会作用于所有引用该类型的已有企划；名称在组织内必须唯一。
     *
     * @param params 包含类型 ID、所属组织 ID 及新名称的修改参数
     */
    void updateProjectType(ProjectTypeUpdateParamsBO params);

    /**
     * 删除企划类型。
     * <p>
     * 仅允许删除尚未被任何企划引用的类型，删除采用逻辑删除；已被引用的类型应改为禁用。
     *
     * @param params 包含企划类型 ID 和当前组织 ID 的删除参数
     */
    void deleteProjectType(ProjectTypeDeleteParamsBO params);

    /**
     * 变更企划类型状态（启用/禁用）。
     * <p>
     * 仅组织管理员可操作，且仅允许操作本组织的类型。
     *
     * @param params 包含类型 ID、目标状态及组织 ID 的变更参数
     */
    void changeProjectTypeStatus(ProjectTypeChangeStatusParamsBO params);
}

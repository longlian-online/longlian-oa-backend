package online.longlian.app.service.orgadmin;

import com.baomidou.mybatisplus.extension.service.IService;
import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectAdminListResultBO;
import online.longlian.app.pojo.bo.orgadmin.ProjectChangeStatusParamsBO;
import online.longlian.app.pojo.entity.Project;

/**
 * 组织管理端企划服务接口。
 * <p>
 * 提供组织管理员视角的企划管理能力，包括分页查询和状态变更。
 * 组织管理员可查看本组织下所有企划（不限创建者），并可启用或禁用企划。
 */
public interface ProjectService extends IService<Project> {

    /**
     * 分页查询组织下的企划列表（管理端）。
     *
     * @param params 包含分页参数、搜索关键词、状态筛选及组织 ID 的查询参数
     * @return 分页的企划管理列表结果
     */
    PageResultBO<ProjectAdminListResultBO> getAdminProjectList(ProjectAdminListParamsBO params);

    /**
     * 变更企划状态。
     * <p>
     * 仅组织管理员可操作，且仅允许操作本组织的企划。
     * 该操作仅变更资源启停状态，不影响企划业务进度。
     *
     * @param params 包含企划 ID、目标状态及组织 ID 的变更参数
     */
    void changeProjectStatus(ProjectChangeStatusParamsBO params);
}

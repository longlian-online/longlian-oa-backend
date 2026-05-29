package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.app.ProjectCreateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectDetailResultBO;
import online.longlian.app.pojo.bo.app.ProjectListParamsBO;
import online.longlian.app.pojo.bo.app.ProjectListResultBO;
import online.longlian.app.pojo.bo.app.ProjectUpdateParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopAddParamsBO;
import online.longlian.app.pojo.bo.app.ProjectWorkshopRemoveParamsBO;
import online.longlian.app.pojo.vo.app.ProjectTypeInfoVO;

import java.util.List;

/**
 * 用户端企划服务接口。
 * <p>
 * 负责企划的浏览、创建、编辑以及个人工坊管理：
 * <ol>
 *   <li><b>企划列表</b>：分页查询，支持关键词搜索、类型筛选、排序，仅返回启用状态的企划</li>
 *   <li><b>企划详情</b>：返回基本信息、类型、封面、进度统计及当前用户的权限标记
 *       （{@code isCreator} / {@code inWorkshop}）</li>
 *   <li><b>企划类型</b>：获取当前组织下所有启用的企划类型列表</li>
 *   <li><b>个人工坊</b>：添加/移除企划至个人工坊，添加操作幂等</li>
 * </ol>
 */
public interface ProjectService {

    /**
     * 分页查询企划列表，支持关键词搜索、类型筛选和排序。
     *
     * @param params 包含分页参数、搜索关键词、类型、排序方式及组织 ID 的查询参数
     * @return 分页的企划列表结果
     */
    PageResultBO<ProjectListResultBO> getProjectList(ProjectListParamsBO params);

    /**
     * 获取企划详情，包含进度统计和当前用户的权限标记。
     * <p>
     * {@code isCreator = true} 时前端展示编辑和分享按钮，
     * {@code inWorkshop = true} 表示当前用户已收藏至工坊。
     *
     * @param projectId 企划 ID
     * @param userId    当前用户 ID
     * @param orgId     当前组织 ID（用于校验企划归属）
     * @return 企划详情，含封面、类型名、进度数据及用户权限标记
     */
    ProjectDetailResultBO getProjectDetail(Long projectId, Long userId, Long orgId);

    /**
     * 获取当前组织下所有启用的企划类型列表。
     *
     * @param orgId 组织 ID
     * @return 企划类型 ID 和名称列表
     */
    List<ProjectTypeInfoVO> getProjectTypes(Long orgId);

    /**
     * 创建企划，默认状态为「进行中」。
     *
     * @param params 包含标题、类型、描述、封面等信息的创建参数
     */
    void createProject(ProjectCreateParamsBO params);

    /**
     * 更新企划，仅创建者可操作。
     *
     * @param params 包含企划 ID、编辑者 ID 及更新字段的参数
     */
    void updateProject(ProjectUpdateParamsBO params);

    /**
     * 将企划添加至当前用户的个人工坊，已添加则幂等返回。
     *
     * @param params 包含企划 ID 和用户 ID 的添加参数
     */
    void addToWorkshop(ProjectWorkshopAddParamsBO params);

    /**
     * 将企划从当前用户的个人工坊中移除（软删除）。
     *
     * @param params 包含企划 ID 和用户 ID 的移除参数
     */
    void removeFromWorkshop(ProjectWorkshopRemoveParamsBO params);
}

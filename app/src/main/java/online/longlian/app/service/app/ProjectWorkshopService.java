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

/**
 * 用户端个人工坊服务接口。
 * <p>
 * 负责用户个人工坊中的企划收藏和自定义任务模板管理：
 * <ol>
 *   <li><b>工坊企划</b>：查看已收藏的企划列表，支持关键词、类型、「我创建的」等多维筛选，
 *       在内存中完成筛选后分页返回</li>
 *   <li><b>个人任务模板</b>：用户可在工坊内创建个人级别
 *       （{@link online.longlian.common.enumeration.TaskTemplateScope#PERSONAL}）的任务模板，
 *       与组织级模板一并展示，创建项目时可选用</li>
 * </ol>
 */
public interface ProjectWorkshopService extends IService<ProjectWorkshop> {

    /**
     * 分页查询当前用户的工坊企划列表。
     * <p>
     * 先在工坊关系中获取已收藏的企划 ID 集合，再按关键词、类型、
     * 「我创建的」等条件在内存中筛选后分页返回。
     *
     * @param params 包含分页参数、关键词、类型、是否我创建的及用户/组织 ID 的查询参数
     * @return 分页的工坊企划列表
     */
    PageResultBO<WorkshopProjectInfoVO> getMyWorkshopList(WorkshopListParamsBO params);

    /**
     * 分页查询工坊任务模板列表。
     * <p>
     * 合并组织级模板和当前用户创建的个人级模板，按创建时间倒序排列后分页返回。
     *
     * @param params 包含分页参数、关键词、是否我创建的及用户/组织 ID 的查询参数
     * @return 分页的任务模板列表，含模板基本信息和节点数量
     */
    PageResultBO<WorkshopTaskTemplateVO> getWorkshopTaskTemplateList(WorkshopTaskTemplateListParamsBO params);

    /**
     * 创建工坊个人任务模板及其节点。
     * <p>
     * 模板作用域固定为 {@code PERSONAL}，模板创建后默认启用。
     *
     * @param params 包含模板名称、描述、节点列表及创建者 ID 的创建参数
     */
    void createWorkshopTaskTemplate(WorkshopTaskTemplateCreateParamsBO params);

    /**
     * 更新工坊个人任务模板信息和节点列表。
     * <p>
     * 仅允许编辑个人级模板且仅创建者可操作。采用全量替换策略：先软删除旧节点，再批量插入新节点。
     *
     * @param params 包含模板 ID、新名称、新描述、新节点列表及编辑者 ID 的更新参数
     */
    void updateWorkshopTaskTemplate(WorkshopTaskTemplateUpdateParamsBO params);
}

package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateDetailResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateListResultBO;
import online.longlian.app.pojo.bo.orgadmin.TaskTemplateUpdateParamsBO;

/**
 * 组织管理端任务模板服务接口。
 * <p>
 * 负责任务流程模板的增删改查与状态管理，仅组织管理员可操作。
 * 任务模板定义了一组有序的任务节点（{@link online.longlian.app.pojo.entity.TaskTemplateNode}），
 * 作为项目启动时生成任务流的蓝图。每个节点引用一个
 * {@link online.longlian.app.pojo.entity.BaseTask 基础任务} 并指定执行顺序。
 */
public interface TaskTemplateService {

    /**
     * 分页查询任务模板列表。
     *
     * @param params 包含分页参数、名称搜索、状态筛选及组织 ID 的查询参数
     * @return 分页的任务模板列表结果
     */
    PageResultBO<TaskTemplateListResultBO> listTaskTemplates(TaskTemplateListParamsBO params);

    /**
     * 获取任务模板详情，包含模板基本信息和关联的任务节点列表。
     *
     * @param templateId 模板 ID
     * @param orgId      组织 ID（用于校验归属权限）
     * @return 模板详情
     */
    TaskTemplateDetailResultBO getTaskTemplateDetail(Long templateId, Long orgId);

    /**
     * 创建任务模板及其关联的任务节点。
     * <p>
     * 模板默认组织级作用域（{@code ORGANIZATION}）、启用状态，引用计数初始为 0。
     *
     * @param params 包含模板名称、描述、创建者 ID 及节点列表的创建参数
     */
    void createTaskTemplate(TaskTemplateCreateParamsBO params);

    /**
     * 更新任务模板信息和节点列表。
     * <p>
     * 采用全量替换策略：先软删除旧节点，再批量插入新节点。
     *
     * @param params 包含模板 ID、新名称、新描述及新节点列表的更新参数
     */
    void updateTaskTemplate(TaskTemplateUpdateParamsBO params);

    /**
     * 变更任务模板状态（启用/禁用）。
     * <p>
     * 仅组织管理员可操作，已禁用的模板不可再用于创建项目。
     *
     * @param params 包含模板 ID、目标状态及组织 ID 的变更参数
     */
    void changeTaskTemplateStatus(TaskTemplateChangeStatusParamsBO params);
}

package online.longlian.app.service.orgadmin;

import online.longlian.app.pojo.bo.common.PageResultBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskChangeStatusParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskCreateParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListParamsBO;
import online.longlian.app.pojo.bo.orgadmin.BaseTaskListResultBO;

/**
 * 组织管理端基础任务（原子任务）服务接口。
 * <p>
 * 基础任务是任务模板中最小的可执行单元，作为模板节点的引用目标。
 * 每个基础任务包含 {@code metaSchema}（JSON Schema 格式的表单元数据），
 * 前端据此动态渲染任务提交表单。
 * <p>
 * 仅组织管理员可操作，创建后默认启用。
 */
public interface BaseTaskService {

    /**
     * 分页查询组织下的基础任务列表，支持关键词搜索和状态筛选。
     *
     * @param params 包含分页参数、关键词、状态筛选及组织 ID 的查询参数
     * @return 分页的基础任务列表
     */
    PageResultBO<BaseTaskListResultBO> listBaseTasks(BaseTaskListParamsBO params);

    /**
     * 创建基础任务。
     * <p>
     * {@code metaSchema} 为 JSON Schema 格式，用于定义任务提交时的前端表单结构。
     *
     * @param params 包含名称、描述、图标、metaSchema 及创建者 ID 的创建参数
     */
    void createBaseTask(BaseTaskCreateParamsBO params);

    /**
     * 变更基础任务状态（启用/禁用）。
     * <p>
     * 仅组织管理员可操作，且仅允许操作本组织的基础任务。
     *
     * @param params 包含基础任务 ID、目标状态及组织 ID 的变更参数
     */
    void changeBaseTaskStatus(BaseTaskChangeStatusParamsBO params);
}

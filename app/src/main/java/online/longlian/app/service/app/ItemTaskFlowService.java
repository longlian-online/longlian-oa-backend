package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.ItemTaskFlowUpdateParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskFlowVO;

/**
 * 用户端项目任务流服务接口。
 * <p>
 * 负责查询项目的任务流可视化数据：
 * 返回任务流基本信息及各节点的当前执行状态，
 * 包括节点对应的
 * {@link online.longlian.app.pojo.entity.TaskInstance TaskInstance} 的领取人和完成状态。
 */
public interface ItemTaskFlowService {

    /**
     * 获取项目任务流，包含流基本信息及各节点与任务实例的关联状态。
     * <p>
     * 各节点的 {@link online.longlian.common.enumeration.TaskInstanceStatus taskStatus}
     * 反映该节点的当前执行状态：
     * <ul>
     *   <li>{@code PENDING} — 前序节点未完成，当前节点尚未解锁</li>
     *   <li>{@code CLAIMED} — 已被接取，正在执行中</li>
     *   <li>{@code COMPLETED} — 已完成</li>
     * </ul>
     *
     * @param itemId 项目 ID
     * @return 包含任务流名称、描述及各节点状态的可视化数据
     */
    ItemTaskFlowVO getItemTaskFlow(Long itemId, Long orgId);

    /**
     * 企划负责人保存这个项目自己的工作流。
     * 已接取或已有提交的节点不能删除，也不能更换原子任务。
     * 修订号与当前任务不一致时整次拒绝。
     */
    void updateItemTaskFlow(ItemTaskFlowUpdateParamsBO params);
}

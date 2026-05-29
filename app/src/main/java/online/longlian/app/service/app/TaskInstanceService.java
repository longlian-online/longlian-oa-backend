package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;

import java.util.List;

/**
 * 用户端任务实例服务接口。
 * <p>
 * 负责任务实例的全生命周期管理，状态流转如下：
 * <pre>
 *   PENDING  ──claimTask──▶  CLAIMED  ──submitTask──▶  COMPLETED
 *                  ▲              │                          │
 *                  │  abandonTask │                          │
 *                  └──────────────┘                          │
 *                                            resetTask      │ rejectTask
 *                                            (回到 CLAIMED) │ (回到 CLAIMED)
 * </pre>
 * <ul>
 *   <li>接取（claim）：任何待接取任务均可被成员接取</li>
 *   <li>放弃（abandon）：仅接取人可将已接取任务退回待接取状态</li>
 *   <li>提交（submit）：接取人提交任务成果并生成
 *       {@link online.longlian.app.pojo.entity.TaskSubmission TaskSubmission} 记录，
 *       同时累加提交统计</li>
 *   <li>重置（reset）：接取人可在下一个任务被接取前可重置任务并重新提交</li>
 *   <li>打回（reject）：后一节点接取人可将已提交任务打回至已接取状态，
 *       附打回意见（视为审核），同时扣减提交统计</li>
 * </ul>
 */
public interface TaskInstanceService {

    /**
     * 获取任务实例详情，包含最近一次提交的内容数据。
     *
     * @param params 包含任务实例 ID 的查询参数
     * @return 任务实例详情，含提交的 metadata 内容
     */
    TaskInstanceDetailVO getTaskInstanceDetail(TaskInstanceDetailParamsBO params);

    /**
     * 查询指定项目下所有任务实例及其节点信息。
     *
     * @param params 包含项目 ID 的查询参数
     * @return 任务实例列表，含节点名称和基础任务信息
     */
    List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params);

    /**
     * 接取任务：{@code PENDING → CLAIMED}，将接取人设为当前用户。
     *
     * @param params 包含任务实例 ID 和用户 ID 的操作参数
     */
    void claimTask(TaskInstanceOperateParamsBO params);

    /**
     * 放弃任务：{@code CLAIMED → PENDING}，清除接取人，仅接取人可操作。
     *
     * @param params 包含任务实例 ID 和用户 ID 的操作参数
     */
    void abandonTask(TaskInstanceOperateParamsBO params);

    /**
     * 提交任务：{@code CLAIMED → COMPLETED}，生成提交记录并累加提交统计。
     * <p>
     * 仅接取人可提交。
     *
     * @param params 包含任务实例 ID、提交内容及其他元数据的提交参数
     */
    void submitTask(TaskInstanceSubmitParamsBO params);

    /**
     * 重置任务：{@code COMPLETED → CLAIMED}，将提交记录标记为 {@code RESET} 并扣减提交统计。
     * <p>
     * 仅接取人可在下一节点被接取前重置。
     *
     * @param params 包含任务实例 ID 和用户 ID 的操作参数
     */
    void resetTask(TaskInstanceOperateParamsBO params);

    /**
     * 打回任务：{@code COMPLETED → CLAIMED}，将提交记录标记为 {@code REJECTED} 并附打回意见。
     * <p>
     * 通常由后一节点的接取人操作，可附带打回意见（视为审核流程）。
     * 打回后任务回到已接取状态等待重新提交，同时扣减提交统计。
     *
     * @param params 包含任务实例 ID、操作者 ID 及打回意见的打回参数
     */
    void rejectTask(TaskInstanceRejectParamsBO params);
}

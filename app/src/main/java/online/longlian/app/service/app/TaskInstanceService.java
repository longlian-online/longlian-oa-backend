package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskInstanceDetailParamsBO;
import online.longlian.app.pojo.bo.app.TaskClaimResultBO;
import online.longlian.app.pojo.bo.app.TaskInstanceListParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAssignParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceRejectParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskInstanceProxySubmitParamsBO;
import online.longlian.app.pojo.bo.app.TaskSubmissionVersionsParamsBO;
import online.longlian.app.pojo.vo.app.ItemTaskInstanceVO;
import online.longlian.app.pojo.vo.app.TaskInstanceDetailVO;
import online.longlian.app.pojo.vo.app.TaskSubmissionVersionVO;

import java.util.List;

/**
 * 用户端任务实例服务接口。
 * <p>
 * 负责任务实例的全生命周期管理，状态流转如下：
 * <pre>
 *   PENDING  ──claimTask / assignTask──▶  CLAIMED  ──submitTask──▶  COMPLETED
 *      │
 *      └──proxySubmitTask──▶ COMPLETED
 *                  ▲              │                          │
 *                  │  abandonTask │                          │
 *                  └──────────────┘                          │
 *                                            resetTask      │ rejectTask
 *                                            (回到 CLAIMED) │ (回到 CLAIMED)
 * </pre>
 * <ul>
 *   <li>接取（claim）：任何待接取任务均可被成员接取</li>
 *   <li>分配（assign）：仅企划负责人可将待接取任务直接分给启用组员，组员无需再领取</li>
 *   <li>放弃（abandon）：仅接取人可将已接取任务退回待接取状态</li>
 *   <li>提交（submit）：接取人提交任务成果并生成
 *       {@link online.longlian.app.pojo.entity.TaskSubmission TaskSubmission} 记录，
 *       同时累加提交统计</li>
 *   <li>代提交（proxySubmit）：仅企划负责人把未接取任务记到指定组员名下，提交时间使用设定值</li>
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
     * 获取任务实例的全部提交版本，按版本号倒序。
     * <p>
     * 每个版本包含提交人/打回人与按表单快照组织的展示字段；存量数据无法回放时
     * 该版本标记 {@code unreadable}。
     *
     * @param params 包含任务实例 ID 的查询参数
     * @return 版本列表；未提交过时为空列表
     */
    List<TaskSubmissionVersionVO> listSubmissionVersions(TaskSubmissionVersionsParamsBO params);

    /**
     * 查询指定项目下所有任务实例及其节点信息。
     *
     * @param params 包含项目 ID 的查询参数
     * @return 任务实例列表，含节点名称和基础任务信息
     */
    List<ItemTaskInstanceVO> listItemTaskInstances(TaskInstanceListParamsBO params);

    /**
     * 接取任务：{@code PENDING → CLAIMED}，将接取人设为当前用户，并按需加入企划工坊。
     *
     * @param params 包含任务实例 ID 和用户 ID 的操作参数
     * @return 是否在本次接取中实际新增了有效企划工坊关联
     */
    TaskClaimResultBO claimTask(TaskInstanceOperateParamsBO params);

    /**
     * 分配任务：{@code PENDING → CLAIMED}，将接取人设为指定组员，并按需加入企划工坊。
     * 仅企划负责人可操作；已接取或已完成的任务不能再次分配。
     *
     * @param params 包含操作人、被分配组员和当前修订号
     */
    TaskClaimResultBO assignTask(TaskInstanceAssignParamsBO params);


    /**
     * 放弃任务：{@code CLAIMED → PENDING}，清除接取人，接取人或企划创建人可操作。
     *
     * @param params 包含当前修订号；主动放弃与取消他人接取均无需填写原因
     */
    void abandonTask(TaskInstanceAbandonParamsBO params);

    /**
     * 提交任务：{@code CLAIMED → COMPLETED}，生成提交记录并累加提交统计。
     * <p>
     * 仅接取人可提交。
     *
     * @param params 包含任务实例 ID、提交内容及其他元数据的提交参数
     */
    void submitTask(TaskInstanceSubmitParamsBO params);

    /**
     * 代提交：{@code PENDING → COMPLETED}。仅企划负责人可把未接取任务记到指定组员名下，
     * 提交时间使用设定值。组员必须是当前组织的启用成员，不要求已加入该企划，也不会自动加入。
     */
    void proxySubmitTask(TaskInstanceProxySubmitParamsBO params);

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
     * 企划创建人或下一阶段执行人可操作，必须匹配当前修订号并填写打回意见。
     * 两项操作均禁止后续阶段存在完成任务；打回精确标记唯一当前提交，按实际提交人扣减统计。
     *
     * @param params 包含任务实例 ID、操作者 ID 及打回意见的打回参数
     */
    void rejectTask(TaskInstanceRejectParamsBO params);

}

package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenTaskAbandonParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmitParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmitResultBO;

/**
 * 开放面任务流转（接取/放弃/提交）。状态机、revision 乐观锁、自动入组、
 * metaSchema 校验与提交版本写入全部复用站内 TaskInstanceService，开放面不加特权。
 */
public interface OpenTaskCommandService {

    /**
     * 接取任务，同站内规则；返回本次是否自动加入企划。
     */
    OpenTaskClaimResultBO claimTask(OpenTaskClaimParamsBO params);

    /**
     * 放弃任务，同站内规则（须匹配 expectedRevision）；成功无业务数据。
     */
    void abandonTask(OpenTaskAbandonParamsBO params);

    /**
     * 提交任务，同站内规则；返回本次提交的版本号。
     */
    OpenTaskSubmitResultBO submitTask(OpenTaskSubmitParamsBO params);
}

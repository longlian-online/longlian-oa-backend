package online.longlian.app.service.open;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ItemMapper;
import online.longlian.app.mapper.ProjectMapper;
import online.longlian.app.mapper.TaskInstanceMapper;
import online.longlian.app.mapper.TaskSubmissionMapper;
import online.longlian.app.pojo.bo.app.taskinstance.TaskClaimResultBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceAbandonParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceOperateParamsBO;
import online.longlian.app.pojo.bo.app.taskinstance.TaskInstanceSubmitParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskAbandonParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskClaimResultBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmitParamsBO;
import online.longlian.app.pojo.bo.open.OpenTaskSubmitResultBO;
import online.longlian.app.pojo.entity.Item;
import online.longlian.app.pojo.entity.Project;
import online.longlian.app.pojo.entity.TaskInstance;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.service.app.taskinstance.TaskInstanceService;
import online.longlian.common.enumeration.Status;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * 开放面任务流转（/open/v1/tasks/{instanceId}/claim|abandon|submit）。
 * 跨模块只读（standard #9）：开放面契约要求跨组织与不存在统一 NOT_FOUND、不泄露存在性，
 * 而站内接取/提交链对组织不符抛的是无权操作（403 语义），因此在进入站内前先做
 * instance→item→project→org 的只读归属校验把两者归一为 NOT_FOUND；
 * 状态机、revision 乐观锁、自动入组、metaSchema 校验、附件绑定与提交版本写入
 * 全部走站内 TaskInstanceService，本域不复制任何业务规则（standard #7/#10）。
 * 提交版本号回查也属于该只读例外：站内提交返回 void，版本号是幂等快照必需的稳定出参。
 */
@Service
@RequiredArgsConstructor
public class OpenTaskCommandServiceImpl implements OpenTaskCommandService {

    private final TaskInstanceService taskInstanceService;
    private final TaskInstanceMapper taskInstanceMapper;
    private final ItemMapper itemMapper;
    private final ProjectMapper projectMapper;
    private final TaskSubmissionMapper taskSubmissionMapper;

    @Override
    public OpenTaskClaimResultBO claimTask(OpenTaskClaimParamsBO params) {
        requireOrgVisibleInstance(params.instanceId(), params.orgId());
        TaskClaimResultBO result = taskInstanceService.claimTask(
                TaskInstanceOperateParamsBO.builder()
                        .instanceId(params.instanceId())
                        .userId(params.userId())
                        .orgId(params.orgId())
                        .build());
        return new OpenTaskClaimResultBO(result.isAutoJoinedProject());
    }

    @Override
    public void abandonTask(OpenTaskAbandonParamsBO params) {
        requireOrgVisibleInstance(params.instanceId(), params.orgId());
        taskInstanceService.abandonTask(
                TaskInstanceAbandonParamsBO.builder()
                        .instanceId(params.instanceId())
                        .userId(params.userId())
                        .orgId(params.orgId())
                        .expectedRevision(params.expectedRevision().toString())
                        .build());
    }

    @Override
    public OpenTaskSubmitResultBO submitTask(OpenTaskSubmitParamsBO params) {
        requireOrgVisibleInstance(params.instanceId(), params.orgId());
        taskInstanceService.submitTask(
                TaskInstanceSubmitParamsBO.builder()
                        .instanceId(params.instanceId())
                        .userId(params.userId())
                        .orgId(params.orgId())
                        .values(params.values())
                        .build());
        // 站内提交返回 void，提交记录与幂等快照落在同一编排事务内，回查最新版本号作幂等快照内容。
        // 站内流转在 REQUIRES_NEW 事务提交后才回到本事务，而本事务 RR 快照已停在查重时刻，
        // 普通 SELECT 看不到新提交行，必须用锁定读取最新已提交数据。
        TaskSubmission latest = taskSubmissionMapper.selectOne(
                new LambdaQueryWrapper<TaskSubmission>()
                        .select(TaskSubmission::getVersionNo)
                        .eq(TaskSubmission::getTaskInstanceId, params.instanceId())
                        .orderByDesc(TaskSubmission::getVersionNo)
                        .last("LIMIT 1 FOR UPDATE"));
        if (latest == null || latest.getVersionNo() == null) {
            throw new AppException(ResultCode.FAIL, "提交记录缺失");
        }
        return new OpenTaskSubmitResultBO(latest.getVersionNo());
    }

    /**
     * 组织归属只读校验：不存在、已删除、跨组织或企划不可用统一 NOT_FOUND，不泄露存在性。
     */
    private void requireOrgVisibleInstance(Long instanceId, Long orgId) {
        TaskInstance instance = taskInstanceMapper.selectById(instanceId);
        if (instance == null || instance.getDeletedAt() != null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务不存在");
        }
        Item item = itemMapper.selectById(instance.getItemId());
        if (item == null || item.getDeletedAt() != null) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务不存在");
        }
        Project project = projectMapper.selectById(item.getProjectId());
        if (project == null || project.getDeletedAt() != null
                || !Objects.equals(project.getOrgId(), orgId)
                || project.getResourceStatus() != Status.ENABLED) {
            throw new AppException(ResultCode.DATA_NOT_EXIT, "任务不存在");
        }
    }

}

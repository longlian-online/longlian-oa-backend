package online.longlian.app.service.app.impl.taskdraft;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.constants.TaskDraftConstants;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.TaskDraftMapper;
import online.longlian.app.pojo.bo.app.TaskDraftConsumeParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftDiscardParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryResultBO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.entity.TaskDraft;
import online.longlian.app.service.app.TaskDraftRecordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.TaskDraftStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TaskDraftRecordServiceImpl extends ServiceImpl<TaskDraftMapper, TaskDraft>
        implements TaskDraftRecordService {

    private final ResourceService resourceService;
    private final Clock clock;

    @Override
    public TaskDraftRecordResultBO getTaskDraftRecord(TaskDraftRecordGetParamsBO params) {
        return toResult(latestDraft(params.getInstanceId(), params.getOrgId()));
    }

    @Override
    public Map<Long, TaskDraftSummaryResultBO> getTaskDraftSummaries(TaskDraftSummaryParamsBO params) {
        if (params.getInstanceIds().isEmpty()) {
            return Map.of();
        }
        Map<Long, TaskDraft> latestByInstance = new HashMap<>();
        for (TaskDraft draft : list(new LambdaQueryWrapper<TaskDraft>()
                .in(TaskDraft::getTaskInstanceId, params.getInstanceIds())
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .eq(TaskDraft::getOwnerId, params.getUserId()))) {
            latestByInstance.merge(draft.getTaskInstanceId(), draft, (current, candidate) ->
                    current.getRevision() >= candidate.getRevision() ? current : candidate);
        }
        Map<Long, TaskDraftSummaryResultBO> summaries = new HashMap<>();
        latestByInstance.forEach((instanceId, draft) -> {
            if (draft.getStatus() == TaskDraftStatus.ACTIVE) {
                summaries.put(instanceId, new TaskDraftSummaryResultBO(draft.getUpdatedAt()));
            }
        });
        return summaries;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TaskDraftRecordResultBO saveTaskDraftRecord(TaskDraftRecordSaveParamsBO params) {
        TaskDraft current = latestDraft(params.getInstanceId(), params.getOrgId());
        TaskDraftRevisionPolicy.requireMatchingRevision(toResult(current), params.getExpectedDraftRevision());
        LocalDateTime now = LocalDateTime.now(clock);
        // 每次暂存新插入一行。上一份记录先留着，是否废弃附件由业务另决定。
        TaskDraft draft = TaskDraft.builder()
                .taskInstanceId(params.getInstanceId())
                .orgId(params.getOrgId())
                .ownerId(params.getUserId())
                .metadata(params.getMetadata())
                .revision(current == null ? 1L : Math.addExact(current.getRevision(), 1))
                .taskRevision(Long.valueOf(params.getExpectedTaskRevision()))
                .status(TaskDraftStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        try {
            save(draft);
        } catch (DuplicateKeyException exception) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
        return toResult(draft);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeTaskDraftRecord(TaskDraftConsumeParamsBO params) {
        // 任务流编辑会递增任务修订号，提交幂等凭据须记录实际提交前的版本。
        boolean consumed = update(new LambdaUpdateWrapper<TaskDraft>()
                .eq(TaskDraft::getId, params.getDraftId())
                .eq(TaskDraft::getRevision, params.getExpectedRevision())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)
                .set(TaskDraft::getMetadata, null)
                .set(TaskDraft::getStatus, TaskDraftStatus.CONSUMED)
                .set(TaskDraft::getSubmittedSubmissionId, params.getSubmissionId())
                .set(TaskDraft::getTaskRevision, params.getTaskRevision())
                .set(TaskDraft::getUpdatedAt, LocalDateTime.now(clock)));
        if (!consumed) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void discardTaskDrafts(TaskDraftDiscardParamsBO params) {
        if (params.getInstanceIds().isEmpty()) {
            return;
        }
        // 删除项目时可能已建立一致性快照，使用当前读获取锁等待后最新的草稿。
        List<TaskDraft> drafts = list(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .in(TaskDraft::getTaskInstanceId, params.getInstanceIds())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)
                .orderByAsc(TaskDraft::getId)
                .last("FOR UPDATE"));
        TaskDraft latest = drafts.stream().max(Comparator.comparing(TaskDraft::getRevision)).orElseThrow();
        LocalDateTime now = LocalDateTime.now(clock);
        for (TaskDraft draft : drafts) {
            resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                    .orgId(params.getOrgId())
                    .bizType("task_draft")
                    .bizIds(List.of(draft.getId()))
                    .build());
            LambdaUpdateWrapper<TaskDraft> update = new LambdaUpdateWrapper<TaskDraft>()
                    .eq(TaskDraft::getId, draft.getId())
                    .set(TaskDraft::getMetadata, null)
                    .set(TaskDraft::getStatus, TaskDraftStatus.DISCARDED)
                    .set(TaskDraft::getUpdatedAt, now);
            // 只推进最新修订号，避免多行同时加一撞上唯一索引。
            if (draft.getId().equals(latest.getId())) {
                update.set(TaskDraft::getRevision, Math.addExact(draft.getRevision(), 1));
            }
            update(update);
        }
    }

    private TaskDraft latestDraft(Long instanceId, Long orgId) {
        return getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getTaskInstanceId, instanceId)
                .eq(TaskDraft::getOrgId, orgId)
                .orderByDesc(TaskDraft::getRevision)
                .last("LIMIT 1"));
    }

    private TaskDraftRecordResultBO toResult(TaskDraft draft) {
        if (draft == null) {
            return null;
        }
        TaskDraftRecordResultBO result = new TaskDraftRecordResultBO();
        BeanUtils.copyProperties(draft, result);
        return result;
    }
}

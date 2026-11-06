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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskDraftRecordServiceImpl extends ServiceImpl<TaskDraftMapper, TaskDraft>
        implements TaskDraftRecordService {

    private final ResourceService resourceService;
    private final Clock clock;

    @Override
    public TaskDraftRecordResultBO getTaskDraftRecord(TaskDraftRecordGetParamsBO params) {
        TaskDraft draft = getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getTaskInstanceId, params.getInstanceId())
                .eq(TaskDraft::getOrgId, params.getOrgId()));
        return toResult(draft);
    }

    @Override
    public Map<Long, TaskDraftSummaryResultBO> getTaskDraftSummaries(TaskDraftSummaryParamsBO params) {
        if (params.getInstanceIds().isEmpty()) {
            return Map.of();
        }
        return list(new LambdaQueryWrapper<TaskDraft>()
                .select(TaskDraft::getTaskInstanceId, TaskDraft::getUpdatedAt)
                .in(TaskDraft::getTaskInstanceId, params.getInstanceIds())
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .eq(TaskDraft::getOwnerId, params.getUserId())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)).stream()
                .collect(Collectors.toMap(TaskDraft::getTaskInstanceId,
                        draft -> new TaskDraftSummaryResultBO(draft.getUpdatedAt())));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public TaskDraftRecordResultBO saveTaskDraftRecord(TaskDraftRecordSaveParamsBO params) {
        TaskDraft draft = getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getTaskInstanceId, params.getInstanceId())
                .eq(TaskDraft::getOrgId, params.getOrgId()));
        TaskDraftRevisionPolicy.requireMatchingRevision(toResult(draft), params.getExpectedDraftRevision());
        LocalDateTime now = LocalDateTime.now(clock);
        if (draft == null) {
            draft = TaskDraft.builder()
                    .taskInstanceId(params.getInstanceId())
                    .revision(0L)
                    .createdAt(now)
                    .build();
        }
        draft.setOrgId(params.getOrgId());
        draft.setOwnerId(params.getUserId());
        draft.setMetadata(params.getMetadata());
        draft.setRevision(Math.addExact(draft.getRevision(), 1));
        draft.setTaskRevision(Long.valueOf(params.getExpectedTaskRevision()));
        draft.setStatus(TaskDraftStatus.ACTIVE);
        draft.setSubmittedSubmissionId(null);
        draft.setUpdatedAt(now);
        if (draft.getId() == null) {
            save(draft);
        } else {
            // Wrapper 显式写入 null，清除上一轮提交记录的关联。
            update(new LambdaUpdateWrapper<TaskDraft>()
                    .eq(TaskDraft::getId, draft.getId())
                    .set(TaskDraft::getOwnerId, draft.getOwnerId())
                    .set(TaskDraft::getMetadata, draft.getMetadata())
                    .set(TaskDraft::getRevision, draft.getRevision())
                    .set(TaskDraft::getTaskRevision, draft.getTaskRevision())
                    .set(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)
                    .set(TaskDraft::getSubmittedSubmissionId, null)
                    .set(TaskDraft::getUpdatedAt, now));
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
        for (TaskDraft draft : drafts) {
            resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                    .orgId(params.getOrgId())
                    .bizType("task_draft")
                    .bizIds(List.of(draft.getId()))
                    .build());
            update(new LambdaUpdateWrapper<TaskDraft>()
                    .eq(TaskDraft::getId, draft.getId())
                    .set(TaskDraft::getMetadata, null)
                    .set(TaskDraft::getStatus, TaskDraftStatus.DISCARDED)
                    .set(TaskDraft::getRevision, Math.addExact(draft.getRevision(), 1))
                    .set(TaskDraft::getUpdatedAt, LocalDateTime.now(clock)));
        }
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

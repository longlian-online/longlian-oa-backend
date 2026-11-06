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
import online.longlian.app.pojo.bo.app.TaskDraftRecordGetByIdParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordResultBO;
import online.longlian.app.pojo.bo.app.TaskDraftRecordSaveParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryParamsBO;
import online.longlian.app.pojo.bo.app.TaskDraftSummaryResultBO;
import online.longlian.app.pojo.bo.common.LockedResourceBO;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.bo.common.ResourceActivateUnboundParamsBO;
import online.longlian.app.pojo.bo.common.ResourceRebindParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileBindParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePromoteParamsBO;
import online.longlian.app.pojo.entity.TaskDraft;
import online.longlian.app.service.app.TaskDraftRecordService;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.TaskDraftStatus;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskDraftRecordServiceImpl extends ServiceImpl<TaskDraftMapper, TaskDraft>
        implements TaskDraftRecordService {

    private final ResourceService resourceService;
    private final Clock clock;

    @Override
    public TaskDraftRecordResultBO getTaskDraftRecord(TaskDraftRecordGetParamsBO params) {
        return toResult(getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getTaskInstanceId, params.getInstanceId())
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)));
    }

    @Override
    public TaskDraftRecordResultBO getTaskDraftRecordById(TaskDraftRecordGetByIdParamsBO params) {
        return toResult(getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getId, params.getDraftId())
                .eq(TaskDraft::getOrgId, params.getOrgId())));
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
        TaskDraft current = getOne(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getTaskInstanceId, params.getInstanceId())
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE));
        String currentId = current == null ? null : current.getId().toString();
        if (!Objects.equals(currentId, params.getExpectedDraftId())) {
            throw new AppException(ResultCode.OPERATION_FAIL, TaskDraftConstants.CONFLICT_MESSAGE);
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (current != null) {
            update(new LambdaUpdateWrapper<TaskDraft>()
                    .eq(TaskDraft::getId, current.getId())
                    .set(TaskDraft::getStatus, TaskDraftStatus.REPLACED)
                    .set(TaskDraft::getUpdatedAt, now));
        }
        TaskDraft draft = TaskDraft.builder()
                .taskInstanceId(params.getInstanceId())
                .orgId(params.getOrgId())
                .ownerId(params.getUserId())
                .metadata(params.getMetadata())
                .status(TaskDraftStatus.ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build();
        save(draft);
        return toResult(draft);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void consumeTaskDraftRecord(TaskDraftConsumeParamsBO params) {
        boolean consumed = update(new LambdaUpdateWrapper<TaskDraft>()
                .eq(TaskDraft::getId, params.getDraftId())
                .eq(TaskDraft::getStatus, TaskDraftStatus.ACTIVE)
                .set(TaskDraft::getStatus, TaskDraftStatus.CONSUMED)
                .set(TaskDraft::getSubmittedSubmissionId, params.getSubmissionId())
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
        // 外层持有任务流锁；当前读避免等待锁前的快照遗漏刚保存的草稿。
        List<TaskDraft> drafts = list(new LambdaQueryWrapper<TaskDraft>()
                .eq(TaskDraft::getOrgId, params.getOrgId())
                .in(TaskDraft::getTaskInstanceId, params.getInstanceIds())
                .in(TaskDraft::getStatus, List.of(TaskDraftStatus.ACTIVE, TaskDraftStatus.REPLACED))
                .orderByAsc(TaskDraft::getId)
                .last("FOR UPDATE"));
        if (drafts.isEmpty()) {
            return;
        }
        resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                .orgId(params.getOrgId()).bizType("task_draft")
                .bizIds(drafts.stream().map(TaskDraft::getId).toList()).build());
        for (TaskDraft draft : drafts) {
            update(new LambdaUpdateWrapper<TaskDraft>()
                    .eq(TaskDraft::getId, draft.getId())
                    .set(TaskDraft::getStatus, TaskDraftStatus.DISCARDED)
                    .set(TaskDraft::getUpdatedAt, LocalDateTime.now(clock)));
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void abandonTaskInstanceResources(TaskDraftDiscardParamsBO params) {
        if (params.getInstanceIds().isEmpty()) {
            return;
        }
        discardTaskDrafts(params);
        resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                .orgId(params.getOrgId())
                .bizType("task_submit")
                .bizIds(params.getInstanceIds())
                .build());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void bindTaskDraftFiles(TaskDraftFileBindParamsBO params) {
        List<LockedResourceBO> resources = lockRequestedFiles(params.getFileIds());
        for (LockedResourceBO resource : resources) {
            if (!Objects.equals(resource.getCreatorId(), params.getUserId())
                    || !Objects.equals(resource.getOrgId(), params.getOrgId())
                    || resource.getStorageCleanedAt() != null) {
                throw unauthorized();
            }
            if (isUnboundTaskUpload(resource)) {
                resourceService.activateUnbound(ResourceActivateUnboundParamsBO.builder()
                        .resourceId(resource.getId()).creatorId(params.getUserId()).orgId(params.getOrgId())
                        .currentBizType("task_submit").targetBizType("task_draft")
                        .targetBizId(params.getDraftId()).build());
            } else if (isRetainedDraftFile(resource, params.getPreviousDraftId())) {
                resourceService.rebindActivated(ResourceRebindParamsBO.builder()
                        .resourceId(resource.getId()).currentBizType("task_draft")
                        .currentBizId(params.getPreviousDraftId()).targetBizType("task_draft")
                        .targetBizId(params.getDraftId()).probe(false).build());
            } else {
                throw unauthorized();
            }
        }
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void promoteTaskDraftFiles(TaskDraftFilePromoteParamsBO params) {
        for (LockedResourceBO resource : lockRequestedFiles(params.getFileIds())) {
            if (!Objects.equals(resource.getCreatorId(), params.getUserId())
                    || !Objects.equals(resource.getOrgId(), params.getOrgId())
                    || !"task_draft".equals(resource.getBizType())
                    || !Objects.equals(resource.getBizId(), params.getDraftId())
                    || resource.getProcessStatus() != FileProcessStatus.Activated
                    || resource.getStorageCleanedAt() != null) {
                throw unauthorized();
            }
            resourceService.rebindActivated(ResourceRebindParamsBO.builder()
                    .resourceId(resource.getId()).currentBizType("task_draft").currentBizId(params.getDraftId())
                    .targetBizType("task_submit").targetBizId(params.getInstanceId()).probe(true).build());
        }
    }

    private List<LockedResourceBO> lockRequestedFiles(List<Long> fileIds) {
        List<Long> ids = fileIds.stream().distinct().sorted().toList();
        if (ids.isEmpty()) {
            return List.of();
        }
        List<LockedResourceBO> resources = resourceService.lockResources(ids);
        if (resources.size() != ids.size()) {
            throw unauthorized();
        }
        return resources;
    }

    private boolean isUnboundTaskUpload(LockedResourceBO resource) {
        return "task_submit".equals(resource.getBizType())
                && Objects.equals(resource.getBizId(), 0L)
                && (resource.getProcessStatus() == FileProcessStatus.Pending
                        || resource.getProcessStatus() == FileProcessStatus.Uploaded);
    }

    private boolean isRetainedDraftFile(LockedResourceBO resource, Long previousDraftId) {
        return previousDraftId != null
                && "task_draft".equals(resource.getBizType())
                && Objects.equals(resource.getBizId(), previousDraftId)
                && resource.getProcessStatus() == FileProcessStatus.Activated;
    }

    private AppException unauthorized() {
        return new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
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

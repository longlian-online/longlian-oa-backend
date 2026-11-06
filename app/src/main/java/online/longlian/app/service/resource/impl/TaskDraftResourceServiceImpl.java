package online.longlian.app.service.resource.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.ActivatedResource;
import online.longlian.app.pojo.bo.common.ResourceAbandonParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCopyParamsBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileCancelParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileCommitParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileListParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePrepareParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePrepareResultBO;
import online.longlian.app.pojo.bo.common.TaskDraftFileResultBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.ResourceService;
import online.longlian.app.service.resource.StorageService;
import online.longlian.app.service.resource.StorageServiceFactory;
import online.longlian.app.service.resource.TaskDraftResourceService;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TaskDraftResourceServiceImpl extends ServiceImpl<ResourceMapper, Resource>
        implements TaskDraftResourceService {

    private final ResourceMapper resourceMapper;
    private final ResourceService resourceService;
    private final StorageServiceFactory storageServiceFactory;
    private final StorageProperties storageProperties;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public TaskDraftFilePrepareResultBO prepareTaskDraftFiles(TaskDraftFilePrepareParamsBO params) {
        String operationToken = UUID.randomUUID().toString();
        Map<Long, Long> fileIdMappings = new LinkedHashMap<>();
        try {
            for (Long fileId : params.getFileIds().stream().distinct().sorted().toList()) {
                Resource sourceResource = resourceMapper.selectById(fileId);
                requireFileOwner(sourceResource, params.getUserId(), params.getOrgId());
                if (isActiveDraftFile(sourceResource, params.getDraftId())) {
                    fileIdMappings.put(fileId, fileId);
                    continue;
                }
                requireUnboundUpload(sourceResource);
                Resource snapshotResource = reserveFileSnapshot(sourceResource, params, operationToken);
                copyFileSnapshot(sourceResource, snapshotResource);
                fileIdMappings.put(fileId, snapshotResource.getId());
            }
            return new TaskDraftFilePrepareResultBO(operationToken, Map.copyOf(fileIdMappings));
        } catch (RuntimeException exception) {
            try {
                cancelTaskDraftFilePreparation(new TaskDraftFileCancelParamsBO(operationToken));
            } catch (RuntimeException cleanupFailure) {
                exception.addSuppressed(cleanupFailure);
            }
            throw exception;
        }
    }

    private Resource reserveFileSnapshot(Resource sourceResource, TaskDraftFilePrepareParamsBO params,
                                         String operationToken) {
        // 先提交租约和快照记录，再复制对象；进程中断后清理任务仍能找到候选对象。
        return new TransactionTemplate(transactionManager).execute(status -> {
            LocalDateTime now = LocalDateTime.now(clock);
            LocalDateTime expiresAt = now.plus(storageProperties.getDraftSealLease());
            int acquired = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                    .eq(Resource::getId, sourceResource.getId())
                    .eq(Resource::getBizId, 0L)
                    .isNull(Resource::getStorageCleanedAt)
                    .in(Resource::getProcessStatus, List.of(FileProcessStatus.Pending, FileProcessStatus.Uploaded))
                    .and(wrapper -> wrapper.isNull(Resource::getDraftSealToken)
                            .or().le(Resource::getDraftSealExpiresAt, now))
                    .set(Resource::getDraftSealToken, operationToken)
                    .set(Resource::getDraftSealExpiresAt, expiresAt));
            if (acquired != 1) {
                throw fileAccessDenied();
            }
            long snapshotId = IdWorker.getId();
            Resource snapshotResource = Resource.builder()
                    .id(snapshotId)
                    .orgId(params.getOrgId())
                    .creatorId(params.getUserId())
                    .storageType(sourceResource.getStorageType())
                    .storageKey("task_draft/" + snapshotId + "." + sourceResource.getFileExt())
                    .fileName(sourceResource.getFileName())
                    .fileExt(sourceResource.getFileExt())
                    .fileSize(sourceResource.getFileSize())
                    .fileMime(sourceResource.getFileMime())
                    .bizType("task_draft")
                    .bizId(0L)
                    .processStatus(FileProcessStatus.Pending)
                    .draftSealToken(operationToken)
                    .draftSealExpiresAt(expiresAt)
                    .draftSealSourceId(sourceResource.getId())
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            resourceMapper.insert(snapshotResource);
            return snapshotResource;
        });
    }

    private void copyFileSnapshot(Resource sourceResource, Resource snapshotResource) {
        StorageService storageService = storageServiceFactory.get(snapshotResource.getStorageType());
        storageService.copyResource(new ResourceCopyParamsBO(
                sourceResource.getStorageKey(), snapshotResource.getStorageKey()));
        storageService.probe(new ResourceProbeParamsBO(snapshotResource.getStorageKey(),
                snapshotResource.getFileSize(), snapshotResource.getFileMime()));
        if (!snapshotResource.getDraftSealExpiresAt().isAfter(LocalDateTime.now(clock))) {
            throw new AppException(ResultCode.OPERATION_FAIL, "附件暂存超时，请重新暂存");
        }
    }

    @Override
    public void cancelTaskDraftFilePreparation(TaskDraftFileCancelParamsBO params) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            releaseSourceLeases(params.getOperationToken());
            resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                    .eq(Resource::getDraftSealToken, params.getOperationToken())
                    .eq(Resource::getBizId, 0L)
                    .isNotNull(Resource::getDraftSealSourceId)
                    .set(Resource::getDraftSealExpiresAt, LocalDateTime.now(clock)));
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void commitTaskDraftFiles(TaskDraftFileCommitParamsBO params) {
        LocalDateTime now = LocalDateTime.now(clock);
        List<Long> fileIds = params.getFileIdMappings().values().stream().distinct().sorted().toList();
        List<Resource> resources = fileIds.isEmpty() ? List.of()
                : resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                        .in(Resource::getId, fileIds)
                        .orderByAsc(Resource::getId)
                        .last("FOR UPDATE"));
        if (resources.size() != fileIds.size()) {
            throw fileAccessDenied();
        }
        for (Resource resource : resources) {
            requireFileOwner(resource, params.getUserId(), params.getOrgId());
            if (isActiveDraftFile(resource, params.getDraftId())) {
                continue;
            }
            if (!"task_draft".equals(resource.getBizType()) || !Objects.equals(resource.getBizId(), 0L)
                    || !params.getOperationToken().equals(resource.getDraftSealToken())
                    || !resource.getDraftSealExpiresAt().isAfter(now)) {
                throw fileAccessDenied();
            }
            int updated = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                    .eq(Resource::getId, resource.getId())
                    .eq(Resource::getBizId, 0L)
                    .eq(Resource::getDraftSealToken, params.getOperationToken())
                    .gt(Resource::getDraftSealExpiresAt, now)
                    .isNull(Resource::getStorageCleanedAt)
                    .eq(Resource::getProcessStatus, FileProcessStatus.Pending)
                    .set(Resource::getBizId, params.getDraftId())
                    .set(Resource::getProcessStatus, FileProcessStatus.Activated)
                    .set(Resource::getDraftSealToken, null)
                    .set(Resource::getDraftSealExpiresAt, null)
                    .set(Resource::getUpdatedAt, now));
            if (updated != 1) {
                throw fileAccessDenied();
            }
        }
        abandonRemovedFiles(params, fileIds);
        releaseSourceLeases(params.getOperationToken());
    }

    private void abandonRemovedFiles(TaskDraftFileCommitParamsBO params, List<Long> retainedFileIds) {
        List<Resource> previousFiles = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getBizType, "task_draft")
                .eq(Resource::getBizId, params.getDraftId())
                .eq(Resource::getOrgId, params.getOrgId())
                .eq(Resource::getProcessStatus, FileProcessStatus.Activated));
        List<Long> removedFileIds = previousFiles.stream().map(Resource::getId)
                .filter(fileId -> !retainedFileIds.contains(fileId)).toList();
        if (!removedFileIds.isEmpty()) {
            resourceService.abandonBound(ResourceAbandonParamsBO.builder()
                    .orgId(params.getOrgId())
                    .resourceIds(removedFileIds)
                    .build());
        }
    }

    private void releaseSourceLeases(String operationToken) {
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getDraftSealToken, operationToken)
                .isNull(Resource::getDraftSealSourceId)
                .set(Resource::getDraftSealToken, null)
                .set(Resource::getDraftSealExpiresAt, null));
    }

    @Override
    public Map<String, TaskDraftFileResultBO> getTaskDraftFiles(TaskDraftFileListParamsBO params) {
        Map<String, TaskDraftFileResultBO> files = new LinkedHashMap<>();
        Map<Long, ActivatedResource> activatedFiles = resourceService.findActivated(
                "task_draft", params.getDraftId(), params.getOrgId(), params.getFileIds());
        for (Long fileId : params.getFileIds()) {
            ActivatedResource resource = activatedFiles.get(fileId);
            if (resource == null) {
                files.put(fileId.toString(), TaskDraftFileResultBO.builder()
                        .fileId(fileId.toString()).fileName("附件不可用").availability("unavailable").build());
                continue;
            }
            var download = resourceService.signDownload(resource);
            files.put(fileId.toString(), TaskDraftFileResultBO.builder()
                    .fileId(fileId.toString())
                    .fileName(resource.fileName())
                    .fileSize(resource.fileSize())
                    .fileMime(resource.fileMime())
                    .availability("available")
                    .readUrl(download.readUrl())
                    .expiresAt(download.expiresAt())
                    .build());
        }
        return files;
    }

    private boolean isActiveDraftFile(Resource resource, Long draftId) {
        return "task_draft".equals(resource.getBizType()) && Objects.equals(resource.getBizId(), draftId)
                && resource.getProcessStatus() == FileProcessStatus.Activated;
    }

    private void requireUnboundUpload(Resource resource) {
        if (!"task_submit".equals(resource.getBizType()) || !Objects.equals(resource.getBizId(), 0L)
                || (resource.getProcessStatus() != FileProcessStatus.Pending
                        && resource.getProcessStatus() != FileProcessStatus.Uploaded)) {
            throw fileAccessDenied();
        }
    }

    private void requireFileOwner(Resource resource, Long userId, Long orgId) {
        if (resource == null || !Objects.equals(resource.getCreatorId(), userId)
                || !Objects.equals(resource.getOrgId(), orgId) || resource.getStorageCleanedAt() != null) {
            throw fileAccessDenied();
        }
    }

    private AppException fileAccessDenied() {
        return new AppException(ResultCode.UNAUTHORIZED_OPERATION, "无权使用该文件");
    }
}

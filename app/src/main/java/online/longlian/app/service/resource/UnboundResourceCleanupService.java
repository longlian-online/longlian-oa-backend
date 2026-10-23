package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnboundResourceCleanupService {
    private static final int BATCH_SIZE = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;
    private static final String MISSING_OBJECT = "对象不存在";
    private static final List<FileProcessStatus> UNBOUND_STATUSES = List.of(
            FileProcessStatus.Pending, FileProcessStatus.Uploaded, FileProcessStatus.Deprecated);

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;
    private final StorageProperties storageProperties;

    public void cleanup(LocalDateTime executeTime) {
        cleanupUnbound(executeTime);
        cleanupAbandoned(executeTime);
        reconcileSizes(executeTime);
    }

    private void cleanupUnbound(LocalDateTime executeTime) {
        long retentionSeconds = Math.max(storageProperties.getUnboundResourceRetention().getSeconds(),
                storageProperties.getPresignedUrlTtlSeconds());
        LocalDateTime cutoff = executeTime.minusSeconds(retentionSeconds);
        List<Resource> candidates = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getBizId, 0L)
                .in(Resource::getProcessStatus, UNBOUND_STATUSES)
                .le(Resource::getCreatedAt, cutoff)
                .isNull(Resource::getStorageCleanedAt)
                .and(wrapper -> wrapper.isNull(Resource::getCleanupNextAt)
                        .or()
                        .le(Resource::getCleanupNextAt, executeTime))
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (Resource candidate : candidates) {
            cleanupUnboundResource(candidate, cutoff, executeTime);
        }
    }

    private void cleanupAbandoned(LocalDateTime executeTime) {
        LocalDateTime cutoff = executeTime.minus(storageProperties.getAbandonedResourceRetention());
        List<Resource> candidates = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .ne(Resource::getBizId, 0L)
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .le(Resource::getDeprecatedAt, cutoff)
                .isNull(Resource::getStorageCleanedAt)
                .and(wrapper -> wrapper.isNull(Resource::getCleanupNextAt)
                        .or()
                        .le(Resource::getCleanupNextAt, executeTime))
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (Resource candidate : candidates) {
            deleteStoredObject(candidate, executeTime, false);
        }
    }

    private void cleanupUnboundResource(Resource resource, LocalDateTime cutoff, LocalDateTime executeTime) {
        // 先用条件更新终止上传生命周期，与业务绑定的状态更新竞争；绑定先成功就绝不删除。
        int claimed = resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resource.getId())
                .eq(Resource::getBizId, 0L)
                .in(Resource::getProcessStatus, UNBOUND_STATUSES)
                .le(Resource::getCreatedAt, cutoff)
                .isNull(Resource::getStorageCleanedAt)
                .and(wrapper -> wrapper.isNull(Resource::getCleanupNextAt)
                        .or()
                        .le(Resource::getCleanupNextAt, executeTime))
                .set(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .set(Resource::getUpdatedAt, executeTime));
        if (claimed != 1) {
            return;
        }
        deleteStoredObject(resource, executeTime, true);
    }

    private void deleteStoredObject(Resource resource, LocalDateTime executeTime, boolean unbound) {
        try {
            storageFactory.get(resource.getStorageType()).delete(resource.getStorageKey());
            markCleaned(resource.getId(), executeTime, unbound);
        } catch (RuntimeException exception) {
            try {
                recordFailure(resource, executeTime, exception, unbound);
            } catch (RuntimeException persistenceException) {
                log.error("资源清理失败且无法记录重试状态: resourceId={}", resource.getId(), persistenceException);
            }
        }
    }

    private void markCleaned(Long resourceId, LocalDateTime executeTime, boolean unbound) {
        resourceMapper.update(null, cleanedUpdate(resourceId, unbound)
                .set(Resource::getStorageCleanedAt, executeTime)
                .set(Resource::getCleanupNextAt, null)
                .set(Resource::getCleanupError, null)
                .set(Resource::getUpdatedAt, executeTime));
    }

    private void recordFailure(Resource resource, LocalDateTime executeTime, RuntimeException exception, boolean unbound) {
        int attempts = resource.getCleanupAttempts() + 1;
        resourceMapper.update(null, cleanedUpdate(resource.getId(), unbound)
                .set(Resource::getCleanupAttempts, attempts)
                .set(Resource::getCleanupNextAt, executeTime.plus(retryDelay(attempts)))
                .set(Resource::getCleanupError, errorMessage(exception))
                .set(Resource::getUpdatedAt, executeTime));
    }

    private LambdaUpdateWrapper<Resource> cleanedUpdate(Long resourceId, boolean unbound) {
        LambdaUpdateWrapper<Resource> wrapper = new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .isNull(Resource::getStorageCleanedAt);
        if (unbound) {
            wrapper.eq(Resource::getBizId, 0L);
        } else {
            wrapper.ne(Resource::getBizId, 0L)
                    .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated);
        }
        return wrapper;
    }

    private void reconcileSizes(LocalDateTime executeTime) {
        Duration interval = storageProperties.getSizeReconcileInterval();
        LocalDateTime due = executeTime.minus(interval);
        List<Resource> candidates = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .isNull(Resource::getStorageCleanedAt)
                .and(wrapper -> wrapper.isNull(Resource::getSizeCheckedAt)
                        .or()
                        .le(Resource::getSizeCheckedAt, due))
                .orderByAsc(Resource::getSizeCheckedAt)
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (Resource candidate : candidates) {
            reconcileSize(candidate, executeTime, due);
        }
    }

    private void reconcileSize(Resource resource, LocalDateTime executeTime, LocalDateTime due) {
        if (uploadStillOpen(resource, executeTime)) {
            // 签名仍有效时对象可能还没传完。把核对时间推到签名过期后，避免占住本批名额。
            touchSizeChecked(resource.getId(), due.plusSeconds(storageProperties.getPresignedUrlTtlSeconds()), executeTime);
            return;
        }
        try {
            OptionalLong length = storageFactory.get(resource.getStorageType()).findContentLength(resource.getStorageKey());
            if (length.isEmpty()) {
                recordMissingObject(resource, executeTime);
                return;
            }
            recordObservedSize(resource, length.getAsLong(), executeTime);
        } catch (RuntimeException exception) {
            log.warn("资源大小核对失败: resourceId={}", resource.getId(), exception);
            touchSizeChecked(resource.getId(), executeTime, executeTime);
        }
    }

    private boolean uploadStillOpen(Resource resource, LocalDateTime executeTime) {
        if (resource.getProcessStatus() != FileProcessStatus.Pending
                && resource.getProcessStatus() != FileProcessStatus.Uploaded) {
            return false;
        }
        return resource.getCreatedAt() != null
                && resource.getCreatedAt().isAfter(executeTime.minusSeconds(storageProperties.getPresignedUrlTtlSeconds()));
    }

    private void recordMissingObject(Resource resource, LocalDateTime executeTime) {
        LambdaUpdateWrapper<Resource> update = new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resource.getId())
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getSizeCheckedAt, executeTime)
                .set(Resource::getUpdatedAt, executeTime);
        if (!missingIsExpected(resource)) {
            update.set(Resource::getCleanupError, MISSING_OBJECT);
        }
        resourceMapper.update(null, update);
    }

    private boolean missingIsExpected(Resource resource) {
        Long bizId = resource.getBizId();
        if (bizId == null || bizId == 0L) {
            return true;
        }
        return resource.getProcessStatus() == FileProcessStatus.Pending
                || resource.getProcessStatus() == FileProcessStatus.Uploaded;
    }

    private void recordObservedSize(Resource resource, long length, LocalDateTime executeTime) {
        LambdaUpdateWrapper<Resource> update = new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resource.getId())
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getSizeCheckedAt, executeTime)
                .set(Resource::getUpdatedAt, executeTime);
        if (resource.getFileSize() == null || resource.getFileSize() != length) {
            update.set(Resource::getFileSize, length);
        }
        if (MISSING_OBJECT.equals(resource.getCleanupError())) {
            update.set(Resource::getCleanupError, null);
        }
        resourceMapper.update(null, update);
    }

    private void touchSizeChecked(Long resourceId, LocalDateTime checkedAt, LocalDateTime executeTime) {
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getSizeCheckedAt, checkedAt)
                .set(Resource::getUpdatedAt, executeTime));
    }

    private Duration retryDelay(int attempts) {
        return switch (Math.min(attempts, 5)) {
            case 1 -> Duration.ofMinutes(5);
            case 2 -> Duration.ofMinutes(15);
            case 3 -> Duration.ofHours(1);
            case 4 -> Duration.ofHours(6);
            default -> Duration.ofHours(24);
        };
    }

    private String errorMessage(RuntimeException exception) {
        String message = exception.getClass().getSimpleName() + ": " + String.valueOf(exception.getMessage());
        return message.length() <= ERROR_MESSAGE_MAX_LENGTH
                ? message
                : message.substring(0, ERROR_MESSAGE_MAX_LENGTH);
    }
}

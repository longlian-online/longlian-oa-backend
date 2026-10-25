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

@Slf4j
@Service
@RequiredArgsConstructor
public class UnboundResourceCleanupService {
    private static final int BATCH_SIZE = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;
    private static final List<FileProcessStatus> UNBOUND_STATUSES = List.of(
            FileProcessStatus.Pending, FileProcessStatus.Uploaded, FileProcessStatus.Deprecated);

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;
    private final StorageProperties storageProperties;

    public void cleanup(LocalDateTime executeTime) {
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
            cleanupResource(candidate, cutoff, executeTime);
        }
    }

    private void cleanupResource(Resource resource, LocalDateTime cutoff, LocalDateTime executeTime) {
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

        try {
            storageFactory.get(resource.getStorageType()).delete(resource.getStorageKey());
            markCleaned(resource.getId(), executeTime);
        } catch (RuntimeException exception) {
            try {
                recordFailure(resource, executeTime, exception);
            } catch (RuntimeException persistenceException) {
                log.error("未绑定资源清理失败且无法记录重试状态: resourceId={}", resource.getId(), persistenceException);
            }
        }
    }

    private void markCleaned(Long resourceId, LocalDateTime executeTime) {
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getBizId, 0L)
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getStorageCleanedAt, executeTime)
                .set(Resource::getCleanupNextAt, null)
                .set(Resource::getCleanupError, null)
                .set(Resource::getUpdatedAt, executeTime));
    }

    private void recordFailure(Resource resource, LocalDateTime executeTime, RuntimeException exception) {
        int attempts = resource.getCleanupAttempts() + 1;
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resource.getId())
                .eq(Resource::getBizId, 0L)
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getCleanupAttempts, attempts)
                .set(Resource::getCleanupNextAt, executeTime.plus(retryDelay(attempts)))
                .set(Resource::getCleanupError, errorMessage(exception))
                .set(Resource::getUpdatedAt, executeTime));
        log.warn("未绑定资源物理清理失败，将重试: resourceId={}, attempts={}", resource.getId(), attempts, exception);
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

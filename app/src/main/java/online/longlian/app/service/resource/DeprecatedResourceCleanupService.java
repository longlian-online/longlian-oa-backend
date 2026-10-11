package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
public class DeprecatedResourceCleanupService {
    private static final int BATCH_SIZE = 100;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 1_000;

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageFactory;

    public void cleanup(LocalDateTime executeTime) {
        List<Resource> candidates = resourceMapper.selectList(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .isNull(Resource::getStorageCleanedAt)
                .and(wrapper -> wrapper.isNull(Resource::getCleanupNextAt)
                        .or()
                        .le(Resource::getCleanupNextAt, executeTime))
                .orderByAsc(Resource::getId)
                .last("LIMIT " + BATCH_SIZE));
        for (Resource candidate : candidates) {
            cleanupResource(candidate.getId(), executeTime);
        }
    }

    private void cleanupResource(Long resourceId, LocalDateTime executeTime) {
        Resource resource = resourceMapper.selectOne(new LambdaQueryWrapper<Resource>()
                .eq(Resource::getId, resourceId)
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .isNull(Resource::getStorageCleanedAt));
        if (resource == null) {
            return;
        }

        try {
            storageFactory.get(resource.getStorageType()).delete(resource.getStorageKey());
            markCleaned(resource.getId(), executeTime);
        } catch (RuntimeException exception) {
            try {
                recordFailure(resource, executeTime, exception);
            } catch (RuntimeException persistenceException) {
                log.error("废弃资源清理失败且无法记录重试状态: resourceId={}", resourceId, persistenceException);
            }
        }
    }

    private void markCleaned(Long resourceId, LocalDateTime executeTime) {
        resourceMapper.update(null, new LambdaUpdateWrapper<Resource>()
                .eq(Resource::getId, resourceId)
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
                .eq(Resource::getProcessStatus, FileProcessStatus.Deprecated)
                .isNull(Resource::getStorageCleanedAt)
                .set(Resource::getCleanupAttempts, attempts)
                .set(Resource::getCleanupNextAt, executeTime.plus(retryDelay(attempts)))
                .set(Resource::getCleanupError, errorMessage(exception))
                .set(Resource::getUpdatedAt, executeTime));
        log.warn("废弃资源物理清理失败，将重试: resourceId={}, attempts={}", resource.getId(), attempts, exception);
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

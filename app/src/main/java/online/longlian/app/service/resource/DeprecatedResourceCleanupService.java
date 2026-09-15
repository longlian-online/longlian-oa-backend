package online.longlian.app.service.resource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.DeprecatedResourceCleanupBO;
import online.longlian.common.enumeration.FileProcessStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeprecatedResourceCleanupService {
    private static final int BATCH_SIZE = 100;
    private static final int MAX_ERROR_LENGTH = 1000;

    private final ResourceMapper resourceMapper;
    private final StorageServiceFactory storageServiceFactory;

    public void cleanup(LocalDateTime executeTime) {
        List<DeprecatedResourceCleanupBO> resources = resourceMapper.selectDeprecatedForCleanup(
                FileProcessStatus.Deprecated, executeTime, BATCH_SIZE
        );
        resources.forEach(resource -> cleanupResource(resource, executeTime));
    }

    private void cleanupResource(DeprecatedResourceCleanupBO candidate, LocalDateTime executeTime) {
        DeprecatedResourceCleanupBO resource = resourceMapper.selectDeprecatedForCleanupById(
                candidate.getId(), FileProcessStatus.Deprecated
        );
        if (resource == null) {
            return;
        }

        try {
            storageServiceFactory.get(resource.getStorageType()).delete(resource.getStorageKey());
            resourceMapper.markStorageCleaned(resource.getId(), FileProcessStatus.Deprecated, executeTime);
        } catch (Exception exception) {
            int attempts = resource.getCleanupAttempts() + 1;
            LocalDateTime nextAttemptAt = executeTime.plusMinutes(retryDelayMinutes(attempts));
            String failure = truncateFailure(exception);
            resourceMapper.recordCleanupFailure(
                    resource.getId(),
                    FileProcessStatus.Deprecated,
                    failure,
                    nextAttemptAt
            );
            log.warn("废弃资源物理清理失败 | resourceId={} storageType={} storageKey={} nextAttemptAt={} error={}",
                    resource.getId(), resource.getStorageType(), resource.getStorageKey(), nextAttemptAt, failure);
        }
    }

    private long retryDelayMinutes(int attempts) {
        return switch (attempts) {
            case 1 -> 5;
            case 2 -> 15;
            case 3 -> 60;
            case 4 -> 360;
            default -> 1440;
        };
    }

    private String truncateFailure(Exception exception) {
        String message = exception.getMessage();
        String failure = exception.getClass().getSimpleName() + (message == null ? "" : ": " + message);
        return failure.length() <= MAX_ERROR_LENGTH ? failure : failure.substring(0, MAX_ERROR_LENGTH);
    }
}

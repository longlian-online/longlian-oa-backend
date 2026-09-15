package online.longlian.app.service.resource;

import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.DeprecatedResourceCleanupBO;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DeprecatedResourceCleanupServiceTest {
    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private StorageServiceFactory storageServiceFactory;
    @Mock
    private StorageService storageService;
    @InjectMocks
    private DeprecatedResourceCleanupService cleanupService;

    @Test
    public void shouldDeleteDeprecatedLocalResourceAndMarkCleaned() {
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 12, 0);
        DeprecatedResourceCleanupBO resource = resource(1L, StorageType.LOCAL, "avatar/1.png", 0);
        when(resourceMapper.selectDeprecatedForCleanup(FileProcessStatus.Deprecated, executeTime, 100))
                .thenReturn(List.of(resource));
        when(resourceMapper.selectDeprecatedForCleanupById(1L, FileProcessStatus.Deprecated)).thenReturn(resource);
        when(storageServiceFactory.get(StorageType.LOCAL)).thenReturn(storageService);

        cleanupService.cleanup(executeTime);

        verify(storageService).delete("avatar/1.png");
        verify(resourceMapper).markStorageCleaned(1L, FileProcessStatus.Deprecated, executeTime);
        verify(resourceMapper, never()).recordCleanupFailure(any(), any(), any(), any());
    }

    @Test
    public void shouldDeleteDeprecatedOssResourceUsingItsPersistedStorageType() {
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 12, 0);
        DeprecatedResourceCleanupBO resource = resource(2L, StorageType.OSS, "cover/2.jpg", 0);
        when(resourceMapper.selectDeprecatedForCleanup(FileProcessStatus.Deprecated, executeTime, 100))
                .thenReturn(List.of(resource));
        when(resourceMapper.selectDeprecatedForCleanupById(2L, FileProcessStatus.Deprecated)).thenReturn(resource);
        when(storageServiceFactory.get(StorageType.OSS)).thenReturn(storageService);

        cleanupService.cleanup(executeTime);

        verify(storageServiceFactory).get(StorageType.OSS);
        verify(storageService).delete("cover/2.jpg");
        verify(resourceMapper).markStorageCleaned(2L, FileProcessStatus.Deprecated, executeTime);
    }

    @Test
    public void shouldRecordRetryableFailureWithoutMarkingResourceCleaned() {
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 12, 0);
        DeprecatedResourceCleanupBO resource = resource(3L, StorageType.LOCAL, "avatar/3.png", 1);
        when(resourceMapper.selectDeprecatedForCleanup(FileProcessStatus.Deprecated, executeTime, 100))
                .thenReturn(List.of(resource));
        when(resourceMapper.selectDeprecatedForCleanupById(3L, FileProcessStatus.Deprecated)).thenReturn(resource);
        when(storageServiceFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        doThrow(new IllegalStateException("storage unavailable")).when(storageService).delete("avatar/3.png");
        ArgumentCaptor<LocalDateTime> nextAttemptAt = ArgumentCaptor.forClass(LocalDateTime.class);

        cleanupService.cleanup(executeTime);

        verify(resourceMapper).recordCleanupFailure(
                eq(3L),
                eq(FileProcessStatus.Deprecated),
                eq("IllegalStateException: storage unavailable"),
                nextAttemptAt.capture()
        );
        assertThat(nextAttemptAt.getValue()).isEqualTo(executeTime.plusMinutes(15));
        verify(resourceMapper, never()).markStorageCleaned(any(), any(), any());
    }

    private DeprecatedResourceCleanupBO resource(Long id, StorageType storageType, String storageKey, int attempts) {
        return new DeprecatedResourceCleanupBO(id, storageType, storageKey, attempts);
    }
}

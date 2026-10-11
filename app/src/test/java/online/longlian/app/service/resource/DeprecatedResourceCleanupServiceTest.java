package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class DeprecatedResourceCleanupServiceTest {
    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private StorageServiceFactory storageFactory;
    @Mock
    private StorageService storageService;

    private DeprecatedResourceCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        cleanupService = new DeprecatedResourceCleanupService(resourceMapper, storageFactory);
    }

    @Test
    void shouldDeleteDeprecatedResourceUsingPersistedStorageType() {
        Resource resource = deprecatedResource(StorageType.COS);
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 8, 0);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.COS)).thenReturn(storageService);

        cleanupService.cleanup(executeTime);

        verify(storageService).delete("avatar/1.png");
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> update = updateCaptor();
        assertThat(update.getValue().getSqlSet())
                .contains("storage_cleaned_at", "cleanup_next_at", "cleanup_error");
    }

    @Test
    void shouldNotDeleteResourceThatIsNoLongerDeprecated() {
        Resource resource = deprecatedResource(StorageType.LOCAL);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.selectOne(any())).thenReturn(null);

        cleanupService.cleanup(LocalDateTime.of(2026, 9, 16, 8, 0));

        verify(storageFactory, never()).get(any());
        verify(resourceMapper, never()).update(isNull(), any());
    }

    @Test
    void shouldKeepFailedCleanupEligibleForRetry() {
        Resource resource = deprecatedResource(StorageType.OSS);
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 8, 0);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        doThrow(new IllegalStateException("object store unavailable"))
                .when(storageService).delete("avatar/1.png");

        cleanupService.cleanup(executeTime);
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> update = updateCaptor();
        assertThat(update.getValue().getSqlSet())
                .contains("cleanup_attempts", "cleanup_next_at", "cleanup_error");
        assertThat(update.getValue().getParamNameValuePairs().values())
                .contains(executeTime.plusMinutes(5));
    }

    @Test
    void shouldIncreaseRetryDelayForRepeatedFailures() {
        LocalDateTime executeTime = LocalDateTime.of(2026, 9, 16, 8, 0);

        assertThat(failedCleanupUpdate(1, new IllegalStateException("first"), executeTime)
                .getParamNameValuePairs().values()).contains(executeTime.plusMinutes(15));
        assertThat(failedCleanupUpdate(2, new IllegalStateException("second"), executeTime)
                .getParamNameValuePairs().values()).contains(executeTime.plusHours(1));
        assertThat(failedCleanupUpdate(3, new IllegalStateException("third"), executeTime)
                .getParamNameValuePairs().values()).contains(executeTime.plusHours(6));
        assertThat(failedCleanupUpdate(4, new IllegalStateException("fourth"), executeTime)
                .getParamNameValuePairs().values()).contains(executeTime.plusHours(24));
    }

    @Test
    void shouldTruncateCleanupFailureMessage() {
        LambdaUpdateWrapper<Resource> update = failedCleanupUpdate(0,
                new IllegalStateException("x".repeat(1_001)), LocalDateTime.of(2026, 9, 16, 8, 0));

        assertThat(update.getParamNameValuePairs().values())
                .contains(("IllegalStateException: " + "x".repeat(1_001)).substring(0, 1_000));
    }

    @Test
    void shouldContinueBatchWhenFailureStateCannotBePersisted() {
        Resource failed = deprecatedResource(1L, "avatar/1.png", StorageType.OSS, 0);
        Resource succeeding = deprecatedResource(2L, "avatar/2.png", StorageType.OSS, 0);
        when(resourceMapper.selectList(any())).thenReturn(List.of(failed, succeeding));
        when(resourceMapper.selectOne(any())).thenReturn(failed, succeeding);
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        doThrow(new IllegalStateException("object store unavailable")).doNothing()
                .when(storageService).delete(any());
        when(resourceMapper.update(isNull(), any())).thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(1);

        cleanupService.cleanup(LocalDateTime.of(2026, 9, 16, 8, 0));

        verify(storageService).delete("avatar/2.png");
        verify(resourceMapper, times(2)).update(isNull(), any());
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaUpdateWrapper<Resource>> updateCaptor() {
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(resourceMapper).update(isNull(), update.capture());
        return update;
    }

    private LambdaUpdateWrapper<Resource> failedCleanupUpdate(int attempts, RuntimeException exception,
            LocalDateTime executeTime) {
        reset(resourceMapper, storageFactory, storageService);
        Resource resource = deprecatedResource(1L, "avatar/1.png", StorageType.OSS, attempts);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        doThrow(exception).when(storageService).delete("avatar/1.png");

        cleanupService.cleanup(executeTime);
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> update = updateCaptor();
        return update.getValue();
    }

    private Resource deprecatedResource(StorageType storageType) {
        return deprecatedResource(1L, "avatar/1.png", storageType, 0);
    }

    private Resource deprecatedResource(Long id, String storageKey, StorageType storageType, int cleanupAttempts) {
        return Resource.builder()
                .id(id)
                .storageType(storageType)
                .storageKey(storageKey)
                .processStatus(FileProcessStatus.Deprecated)
                .cleanupAttempts(cleanupAttempts)
                .build();
    }
}

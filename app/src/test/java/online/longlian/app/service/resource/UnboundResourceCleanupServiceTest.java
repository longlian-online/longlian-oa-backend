package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class UnboundResourceCleanupServiceTest {
    private static final LocalDateTime EXECUTE_TIME = LocalDateTime.of(2026, 10, 20, 12, 0);

    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private StorageServiceFactory storageFactory;
    @Mock
    private StorageService storageService;

    private UnboundResourceCleanupService cleanupService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        cleanupService = new UnboundResourceCleanupService(resourceMapper, storageFactory, new StorageProperties());
    }

    @Test
    void shouldNotDeleteWhenConcurrentBindingWinsTheClaim() {
        when(resourceMapper.selectList(any())).thenReturn(List.of(unboundResource(1L, 0)));
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        cleanupService.cleanup(EXECUTE_TIME);

        verifyNoInteractions(storageFactory, storageService);
    }

    @ParameterizedTest
    @CsvSource({"0,5", "1,15", "2,60", "3,360", "4,1440"})
    void shouldBackOffFailedDeletionBeforeRetrying(int previousAttempts, long delayMinutes) {
        Resource resource = unboundResource(1L, previousAttempts);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.update(isNull(), any())).thenReturn(1);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        doThrow(new IllegalStateException("storage unavailable")).when(storageService).delete(resource.getStorageKey());

        cleanupService.cleanup(EXECUTE_TIME);

        LambdaUpdateWrapper<Resource> failure = lastUpdate();
        failure.getSqlSet();
        assertThat(failure.getParamNameValuePairs().values())
                .contains(EXECUTE_TIME.plusMinutes(delayMinutes), "IllegalStateException: storage unavailable");
    }

    @Test
    void shouldBoundPersistedFailureDetailsToTheDatabaseColumn() {
        Resource resource = unboundResource(1L, 0);
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(resourceMapper.update(isNull(), any())).thenReturn(1);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        doThrow(new IllegalStateException("x".repeat(1_001))).when(storageService).delete(resource.getStorageKey());

        cleanupService.cleanup(EXECUTE_TIME);

        LambdaUpdateWrapper<Resource> failure = lastUpdate();
        failure.getSqlSet();
        assertThat(failure.getParamNameValuePairs().values())
                .contains(("IllegalStateException: " + "x".repeat(1_001)).substring(0, 1_000));
    }

    @Test
    void shouldContinueOtherResourcesWhenFailureStateCannotBePersisted() {
        Resource failed = unboundResource(1L, 0);
        Resource succeeding = unboundResource(2L, 0);
        when(resourceMapper.selectList(any())).thenReturn(List.of(failed, succeeding));
        when(resourceMapper.update(isNull(), any())).thenReturn(1)
                .thenThrow(new IllegalStateException("database unavailable"))
                .thenReturn(1, 1);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        doThrow(new IllegalStateException("storage unavailable")).when(storageService).delete(failed.getStorageKey());

        cleanupService.cleanup(EXECUTE_TIME);

        verify(storageService).delete(succeeding.getStorageKey());
    }

    @SuppressWarnings("unchecked")
    private LambdaUpdateWrapper<Resource> lastUpdate() {
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> updates = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(resourceMapper, times(2)).update(isNull(), updates.capture());
        return updates.getAllValues().getLast();
    }

    private Resource unboundResource(Long id, int attempts) {
        return Resource.builder()
                .id(id)
                .orgId(10L)
                .bizId(0L)
                .storageType(StorageType.LOCAL)
                .storageKey("task_submit/" + id + ".txt")
                .processStatus(FileProcessStatus.Pending)
                .createdAt(EXECUTE_TIME.minusDays(2))
                .cleanupAttempts(attempts)
                .build();
    }
}

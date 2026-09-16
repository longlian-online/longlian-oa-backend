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

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<LambdaUpdateWrapper<Resource>> updateCaptor() {
        ArgumentCaptor<LambdaUpdateWrapper<Resource>> update = ArgumentCaptor.forClass(LambdaUpdateWrapper.class);
        verify(resourceMapper).update(isNull(), update.capture());
        return update;
    }

    private Resource deprecatedResource(StorageType storageType) {
        return Resource.builder()
                .id(1L)
                .storageType(storageType)
                .storageKey("avatar/1.png")
                .processStatus(FileProcessStatus.Deprecated)
                .cleanupAttempts(0)
                .build();
    }
}

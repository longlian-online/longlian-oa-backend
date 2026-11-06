package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.TaskDraftFilePrepareParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.impl.TaskDraftResourceServiceImpl;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TaskDraftResourceServiceTest {

    private final ResourceMapper resourceMapper = mock(ResourceMapper.class);
    private final ResourceService resourceService = mock(ResourceService.class);
    private final StorageServiceFactory storageServiceFactory = mock(StorageServiceFactory.class);
    private final StorageService storageService = mock(StorageService.class);
    private final PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
    private TaskDraftResourceService taskDraftResourceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        taskDraftResourceService = new TaskDraftResourceServiceImpl(resourceMapper, resourceService,
                storageServiceFactory, new StorageProperties(), transactionManager, Clock.systemUTC());
    }

    private Resource createUnboundUpload() {
        return Resource.builder()
                .id(1L)
                .orgId(2L)
                .creatorId(3L)
                .bizType("task_submit")
                .bizId(0L)
                .processStatus(FileProcessStatus.Pending)
                .storageType(StorageType.LOCAL)
                .storageKey("task_submit/1.txt")
                .fileName("result.txt")
                .fileSize(8L)
                .fileMime("text/plain")
                .fileExt("txt")
                .build();
    }

    @Test
    void shouldRegisterSnapshotBeforeCopyingBytes() {
        when(resourceMapper.selectById(1L)).thenReturn(createUnboundUpload());
        when(resourceMapper.update(isNull(), any())).thenReturn(1);
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(storageServiceFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        var result = taskDraftResourceService.prepareTaskDraftFiles(
                new TaskDraftFilePrepareParamsBO(3L, 2L, null, List.of(1L)));
        assertThat(result.getFileIdMappings().get(1L)).isNotEqualTo(1L);
        var order = inOrder(resourceMapper, storageService);
        order.verify(resourceMapper).insert(any(Resource.class));
        order.verify(storageService).copyResource(argThat(params ->
                params.sourceKey().equals("task_submit/1.txt") && params.targetKey().startsWith("task_draft/")));
        order.verify(storageService).probe(any());
    }

    @Test
    void shouldRejectForeignFilesBeforeAnyCopy() {
        when(resourceMapper.selectById(1L)).thenReturn(createUnboundUpload());
        assertThatThrownBy(() -> taskDraftResourceService.prepareTaskDraftFiles(
                new TaskDraftFilePrepareParamsBO(99L, 2L, null, List.of(1L))))
                .isInstanceOf(AppException.class).hasMessageContaining("无权使用");
        verifyNoInteractions(storageServiceFactory, storageService);
    }

    @Test
    void shouldReuseOnlyFilesAlreadyInThisDraft() {
        Resource savedFile = createUnboundUpload();
        savedFile.setBizType("task_draft");
        savedFile.setBizId(8L);
        savedFile.setProcessStatus(FileProcessStatus.Activated);
        when(resourceMapper.selectById(1L)).thenReturn(savedFile);
        assertThat(taskDraftResourceService.prepareTaskDraftFiles(
                new TaskDraftFilePrepareParamsBO(3L, 2L, 8L, List.of(1L))).getFileIdMappings())
                .containsEntry(1L, 1L);
        assertThatThrownBy(() -> taskDraftResourceService.prepareTaskDraftFiles(
                new TaskDraftFilePrepareParamsBO(3L, 2L, 9L, List.of(1L))))
                .isInstanceOf(AppException.class);
        verifyNoInteractions(storageServiceFactory, storageService);
    }

    @Test
    void shouldReleaseLeaseWhenStorageCopyFails() {
        when(resourceMapper.selectById(1L)).thenReturn(createUnboundUpload());
        when(resourceMapper.update(isNull(), any())).thenReturn(1);
        when(transactionManager.getTransaction(any())).thenAnswer(call -> new SimpleTransactionStatus());
        when(storageServiceFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        doThrow(new IllegalStateException("copy failed")).when(storageService).copyResource(any());
        assertThatThrownBy(() -> taskDraftResourceService.prepareTaskDraftFiles(
                new TaskDraftFilePrepareParamsBO(3L, 2L, null, List.of(1L)))).hasMessage("copy failed");
        verify(resourceMapper, times(3)).update(isNull(), any());
    }
}

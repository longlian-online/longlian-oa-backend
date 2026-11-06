package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.TaskDraftFileBindParamsBO;
import online.longlian.app.pojo.bo.common.TaskDraftFilePromoteParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskDraftAttachmentBindingTest {

    @Mock private ResourceMapper mapper;
    @Mock private StorageServiceFactory factory;
    @Mock private StorageService storage;
    private ResourceService service;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        service = new ResourceService(mapper, factory, null, null, new StorageProperties(), Clock.systemUTC());
    }

    private Resource upload() {
        return Resource.builder().id(1L).orgId(2L).creatorId(3L).bizType("task_submit").bizId(0L)
                .processStatus(FileProcessStatus.Pending).storageType(StorageType.LOCAL)
                .storageKey("task_submit/1.txt").fileSize(8L).fileMime("text/plain").build();
    }

    private TaskDraftFileBindParamsBO bindParams(Long previousId) {
        return new TaskDraftFileBindParamsBO(3L, 2L, 11L, previousId, List.of(1L));
    }

    @Test
    void shouldProbeAndActivateNewUploadUsingTheExistingUploadTransition() {
        when(mapper.selectList(any())).thenReturn(List.of(upload()));
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        service.bindTaskDraftFiles(bindParams(null));
        verify(storage).probe(any());
        verify(mapper, times(2)).update(isNull(), any());
    }

    @Test
    void shouldMoveRetainedFileWithoutAnotherStorageOperation() {
        Resource resource = upload();
        resource.setBizType("task_draft");
        resource.setBizId(10L);
        resource.setProcessStatus(FileProcessStatus.Activated);
        when(mapper.selectList(any())).thenReturn(List.of(resource));
        when(mapper.update(isNull(), any())).thenReturn(1);
        service.bindTaskDraftFiles(bindParams(10L));
        verify(mapper).update(isNull(), any());
        verifyNoInteractions(factory, storage);
    }

    @Test
    void shouldRejectFilesFromAnotherDraft() {
        Resource resource = upload();
        resource.setBizType("task_draft");
        resource.setBizId(99L);
        resource.setProcessStatus(FileProcessStatus.Activated);
        when(mapper.selectList(any())).thenReturn(List.of(resource));
        assertThatThrownBy(() -> service.bindTaskDraftFiles(bindParams(10L))).isInstanceOf(AppException.class);
        verify(mapper, never()).update(isNull(), any());
        verifyNoInteractions(factory);
    }

    @Test
    void shouldNotBindAnIncompleteUpload() {
        when(mapper.selectList(any())).thenReturn(List.of(upload()));
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        doThrow(new AppException(online.longlian.app.common.result.ResultCode.OPERATION_FAIL)).when(storage).probe(any());
        assertThatThrownBy(() -> service.bindTaskDraftFiles(bindParams(null))).isInstanceOf(AppException.class);
        verify(mapper, never()).update(isNull(), any());
    }

    @Test
    void shouldPromoteOnlyFilesOwnedByTheSubmittedDraft() {
        Resource resource = upload();
        resource.setBizType("task_draft");
        resource.setBizId(11L);
        resource.setProcessStatus(FileProcessStatus.Activated);
        when(mapper.selectList(any())).thenReturn(List.of(resource));
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        service.promoteTaskDraftFiles(new TaskDraftFilePromoteParamsBO(3L, 2L, 11L, 100L, List.of(1L)));
        verify(storage).probe(any());
        verify(mapper).update(isNull(), any());
    }
}

package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.ResourceActivateUnboundParamsBO;
import online.longlian.app.pojo.bo.common.ResourceRebindParamsBO;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

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

    @Test
    void shouldLockRequestedResourcesInIdOrder() {
        when(mapper.selectList(any())).thenReturn(List.of(upload()));
        assertThat(service.lockResources(List.of(1L))).extracting(resource -> resource.getId()).containsExactly(1L);
    }

    @Test
    void shouldProbeAndActivateNewUploadUsingTheExistingUploadTransition() {
        when(mapper.selectById(1L)).thenReturn(upload());
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        service.activateUnbound(ResourceActivateUnboundParamsBO.builder()
                .resourceId(1L).creatorId(3L).orgId(2L)
                .currentBizType("task_submit").targetBizType("task_draft").targetBizId(11L).build());
        verify(storage).probe(any());
        verify(mapper, times(2)).update(isNull(), any());
    }

    @Test
    void shouldNotActivateAnIncompleteUpload() {
        when(mapper.selectById(1L)).thenReturn(upload());
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        doThrow(new AppException(online.longlian.app.common.result.ResultCode.OPERATION_FAIL)).when(storage).probe(any());
        assertThatThrownBy(() -> service.activateUnbound(ResourceActivateUnboundParamsBO.builder()
                .resourceId(1L).creatorId(3L).orgId(2L)
                .currentBizType("task_submit").targetBizType("task_draft").targetBizId(11L).build()))
                .isInstanceOf(AppException.class);
        verify(mapper, never()).update(isNull(), any());
    }

    @Test
    void shouldRebindActivatedResourceAfterProbe() {
        Resource resource = upload();
        resource.setBizType("task_draft");
        resource.setBizId(11L);
        resource.setProcessStatus(FileProcessStatus.Activated);
        when(mapper.selectById(1L)).thenReturn(resource);
        when(mapper.update(isNull(), any())).thenReturn(1);
        when(factory.get(StorageType.LOCAL)).thenReturn(storage);
        service.rebindActivated(ResourceRebindParamsBO.builder()
                .resourceId(1L).currentBizType("task_draft").currentBizId(11L)
                .targetBizType("task_submit").targetBizId(100L).probe(true).build());
        verify(storage).probe(any());
        verify(mapper).update(isNull(), any());
    }

    @Test
    void shouldMoveActivatedResourceWithoutStorageOperation() {
        when(mapper.update(isNull(), any())).thenReturn(1);
        service.rebindActivated(ResourceRebindParamsBO.builder()
                .resourceId(1L).currentBizType("task_draft").currentBizId(10L)
                .targetBizType("task_draft").targetBizId(11L).probe(false).build());
        verify(mapper).update(isNull(), any());
        verifyNoInteractions(factory, storage);
    }
}

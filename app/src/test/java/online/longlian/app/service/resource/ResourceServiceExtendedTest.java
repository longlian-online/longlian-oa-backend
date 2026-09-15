package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceBindParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.FileProcessStatus;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ResourceServiceExtendedTest {

    @Mock
    private ResourceMapper resourceMapper;
    @Mock
    private StorageServiceFactory storageFactory;
    @Mock
    private StorageService storageService;

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        StorageProperties props = new StorageProperties();
        props.setType(StorageType.OSS);
        props.setOss(new StorageProperties.OssConfig());
        resourceService = new ResourceService(resourceMapper, storageFactory, props);
    }

    @Test
    void getResourceReadUrls_emptyList_returnsEmptyMap() {
        Map<Long, ResourceReadUrlGetResultBO> result = resourceService.getResourceReadUrls(Collections.emptyList());
        assertThat(result).isEmpty();
    }

    @Test
    void getResourceReadUrls_nullList_returnsEmptyMap() {
        Map<Long, ResourceReadUrlGetResultBO> result = resourceService.getResourceReadUrls(null);
        assertThat(result).isEmpty();
    }

    @Test
    void getResourceReadUrls_withResources_returnsUrls() {
        Resource resource = Resource.builder()
                .id(1L).storageKey("avatar/1.png").storageType(StorageType.OSS).orgId(10L)
                .build();
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(storageService.getResourceReadUrl("avatar/1.png")).thenReturn("https://cdn/avatar/1.png");

        Map<Long, ResourceReadUrlGetResultBO> result = resourceService.getResourceReadUrls(List.of(1L));

        assertThat(result).containsKey(1L);
        assertThat(result.get(1L).getUrl()).isEqualTo("https://cdn/avatar/1.png");
    }

    @Test
    void getResourceReadUrl_found_returnsUrl() {
        Resource resource = Resource.builder()
                .id(1L).storageKey("avatar/1.png").storageType(StorageType.OSS).orgId(10L)
                .build();
        when(resourceMapper.selectList(any())).thenReturn(List.of(resource));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(storageService.getResourceReadUrl("avatar/1.png")).thenReturn("https://cdn/avatar/1.png");

        String url = resourceService.getResourceReadUrl(1L);

        assertThat(url).isEqualTo("https://cdn/avatar/1.png");
    }

    @Test
    void getResourceReadUrl_notFound_throws() {
        when(resourceMapper.selectList(any())).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> resourceService.getResourceReadUrl(999L))
                .isInstanceOf(AppException.class);
    }

    @Test
    void bindBizResource_nullBizId_doesNothing() {
        resourceService.bindBizResource(ResourceBindParamsBO.builder().resourceId(1L).build());

        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void bindBizResource_zeroResourceId_doesNothing() {
        resourceService.bindBizResource(ResourceBindParamsBO.builder().resourceId(0L).bizId(1L).build());

        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void bindBizResource_updateFails_throws() {
        Resource pending = pendingResource(1L);
        when(resourceMapper.selectOne(any())).thenReturn(pending);
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(1L).bizId(2L).creatorId(1L).orgId(1L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权使用该文件");
    }

    @Test
    void bindBizResource_activationCompareAndSetFails_throws() {
        Resource uploaded = pendingResource(1L);
        uploaded.setProcessStatus(FileProcessStatus.Uploaded);
        when(resourceMapper.selectOne(any())).thenReturn(uploaded);
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(1L).bizId(2L).creatorId(1L).orgId(1L).build()))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权使用该文件");
    }

    @Test
    void bindBizResource_activatesNewResource() {
        when(resourceMapper.selectOne(any())).thenReturn(pendingResource(1L));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(1, 1);

        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(1L).bizId(2L).creatorId(1L).orgId(1L).build());

        verify(storageService).probe(new ResourceProbeParamsBO("avatar/1.png", 3L, "image/png"));
        verify(resourceMapper, times(2)).update(isNull(), any());
    }

    @Test
    void bindBizResource_replacesActivatedResource() {
        when(resourceMapper.selectOne(any())).thenReturn(pendingResource(2L));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(1, 1, 1);

        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(2L).replacedResourceId(1L).bizId(3L).creatorId(1L).orgId(1L).build());

        verify(resourceMapper, times(3)).update(isNull(), any());
    }

    @Test
    void bindBizResource_sameResourceDoesNotDeprecateIt() {
        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(1L).replacedResourceId(1L).bizId(2L).creatorId(1L).orgId(1L).build());

        verifyNoInteractions(resourceMapper, storageFactory);
    }

    @Test
    void bindBizResource_probeFailureKeepsPendingAndReplacement() {
        when(resourceMapper.selectOne(any())).thenReturn(pendingResource(2L));
        when(storageFactory.get(StorageType.OSS)).thenReturn(storageService);
        doThrow(new AppException(online.longlian.app.common.result.ResultCode.OPERATION_FAIL))
                .when(storageService).probe(any());

        assertThatThrownBy(() -> resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(2L).replacedResourceId(1L).bizId(3L).creatorId(1L).orgId(1L).build()))
                .isInstanceOf(AppException.class);

        verify(storageService).probe(any());
        verify(resourceMapper, never()).update(isNull(), any());
    }

    @Test
    void bindBizResource_uploadedResourceActivatesWithoutAnotherProbe() {
        Resource uploaded = pendingResource(1L);
        uploaded.setProcessStatus(FileProcessStatus.Uploaded);
        when(resourceMapper.selectOne(any())).thenReturn(uploaded);
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(1L).bizId(2L).creatorId(1L).orgId(1L).build());

        verifyNoInteractions(storageFactory);
        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void bindBizResource_zeroNewResourceDeprecatesReplacedResource() {
        resourceService.bindBizResource(ResourceBindParamsBO.builder()
                .resourceId(0L).replacedResourceId(1L).bizId(2L).orgId(1L).build());

        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void shouldRejectAvatarWithNonImageMime() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(
                1L, 10L, "avatar.txt", "txt", 3L, "text/plain", "avatar");

        assertThatThrownBy(() -> resourceService.create(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("只能上传图片");

        verifyNoInteractions(storageFactory, resourceMapper);
    }

    @Test
    void loadPending_found_returnsResource() {
        Resource pending = Resource.builder().id(1L).storageKey("file/1.png").fileSize(3L).build();
        when(resourceMapper.selectOne(any())).thenReturn(pending);

        assertThat(resourceService.loadPending("file/1.png")).isSameAs(pending);
    }

    @Test
    void loadPending_missing_throws() {
        when(resourceMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> resourceService.loadPending("file/1.png"))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权上传或文件已完成上传");
    }

    @Test
    void loadActivated_found_returnsResource() {
        Resource activated = Resource.builder().id(1L).storageKey("file/1.png").build();
        when(resourceMapper.selectOne(any())).thenReturn(activated);

        assertThat(resourceService.loadActivated("file/1.png")).isSameAs(activated);
    }

    @Test
    void loadActivated_missing_throws() {
        when(resourceMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> resourceService.loadActivated("missing.png"))
                .isInstanceOf(AppException.class);
    }

    private Resource pendingResource(Long id) {
        return Resource.builder().id(id).creatorId(1L).orgId(1L).storageKey("avatar/1.png")
                .storageType(StorageType.OSS).fileSize(3L).fileMime("image/png")
                .processStatus(FileProcessStatus.Pending).build();
    }
}

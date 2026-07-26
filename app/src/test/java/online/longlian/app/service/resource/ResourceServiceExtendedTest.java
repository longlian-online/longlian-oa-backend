package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.LocalFileUploadParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.common.ResourceCreateVO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.service.resource.impl.NullStorageService;
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
    void bindBizId_nullResourceId_doesNothing() {
        resourceService.bindBizId(null, 1L, 1L, 1L);
        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void bindBizId_zeroResourceId_doesNothing() {
        resourceService.bindBizId(0L, 1L, 1L, 1L);
        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void bindBizId_nullBizId_doesNothing() {
        resourceService.bindBizId(1L, null, 1L, 1L);
        verify(resourceMapper, never()).update(any(), any());
    }

    @Test
    void bindBizId_updateFails_throws() {
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resourceService.bindBizId(1L, 2L, 1L, 1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权使用该文件");
    }

    @Test
    void bindBizId_updateSucceeds_noException() {
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.bindBizId(1L, 2L, 1L, 1L);

        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void uploadLocalResource_resourceNotFound_throws() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new byte[]{1, 2, 3})
                .build();
        when(resourceMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> resourceService.uploadLocalResource(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权上传该文件");
    }

    @Test
    void uploadLocalResource_sizeMismatch_throws() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new byte[]{1, 2, 3})
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(999L).build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);

        assertThatThrownBy(() -> resourceService.uploadLocalResource(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件大小不匹配");
    }

    @Test
    void uploadLocalResource_valid_uploadsAndActivates() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new byte[]{1, 2, 3})
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(3L).build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.uploadLocalResource(params);

        verify(storageService).upload("file/1.png", new byte[]{1, 2, 3});
        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void getLocalResource_notFound_throws() {
        when(resourceMapper.selectCount(any())).thenReturn(0L);

        assertThatThrownBy(() -> resourceService.getLocalResource("missing.png"))
                .isInstanceOf(AppException.class);
    }

    @Test
    void getLocalResource_found_returnsResource() {
        when(resourceMapper.selectCount(any())).thenReturn(1L);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        org.springframework.core.io.Resource mockResource = mock(org.springframework.core.io.Resource.class);
        when(storageService.getResource("file/1.png")).thenReturn(mockResource);

        org.springframework.core.io.Resource result = resourceService.getLocalResource("file/1.png");

        assertThat(result).isEqualTo(mockResource);
    }
}

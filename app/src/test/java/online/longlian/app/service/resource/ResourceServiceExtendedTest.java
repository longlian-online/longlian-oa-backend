package online.longlian.app.service.resource;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.mapper.ResourceMapper;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileUploadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.common.enumeration.StorageType;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.catchThrowable;
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
    private final LocalFileUrlSigner localFileUrlSigner = new LocalFileUrlSigner(
            "test-local-signing-secret-32-bytes", 300, java.time.Clock.systemUTC());

    private ResourceService resourceService;

    @BeforeEach
    void setUp() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), Resource.class);
        StorageProperties props = new StorageProperties();
        props.setType(StorageType.OSS);
        props.setOss(new StorageProperties.OssConfig());
        resourceService = new ResourceService(resourceMapper, storageFactory, props, localFileUrlSigner);
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
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(3L)
                .build();
        when(resourceMapper.selectOne(any())).thenReturn(null);

        assertThatThrownBy(() -> resourceService.uploadLocalResource(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权上传");
    }

    @Test
    void uploadLocalResource_sizeMismatch_throws() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(3L)
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
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(3L)
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(3L).fileMime("image/png").build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.uploadLocalResource(params);

        verify(storageService).upload(argThat((LocalFileWriteParamsBO writeParams) ->
                writeParams.getStorageKey().equals("file/1.png")
                        && writeParams.getExpectedSize().equals(3L)
                        && writeParams.getExpectedMimeType().equals("image/png")));
        verify(resourceMapper).update(isNull(), any());
    }

    @Test
    void shouldDeleteStoredFileWhenStatusUpdateFails() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(3L)
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(3L).fileMime("image/png").build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(0);

        assertThatThrownBy(() -> resourceService.uploadLocalResource(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件状态更新失败");

        verify(storageService).delete("file/1.png");
    }

    @Test
    void shouldUploadChunkedContentWhenActualSizeMatches() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(-1L)
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(3L).fileMime("image/png").build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(1);

        resourceService.uploadLocalResource(params);

        verify(storageService).upload(any(LocalFileWriteParamsBO.class));
    }

    @Test
    void shouldPreserveStatusFailureWhenStoredFileCleanupFails() {
        LocalFileUploadParamsBO params = LocalFileUploadParamsBO.builder()
                .storageKey("file/1.png").userId(1L).orgId(10L)
                .content(new ByteArrayInputStream(new byte[]{1, 2, 3}))
                .contentLength(3L)
                .build();
        Resource resource = Resource.builder().id(1L).fileSize(3L).fileMime("image/png").build();
        when(resourceMapper.selectOne(any())).thenReturn(resource);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        when(resourceMapper.update(isNull(), any())).thenReturn(0);
        doThrow(new IllegalStateException("delete failed")).when(storageService).delete("file/1.png");

        Throwable failure = catchThrowable(() -> resourceService.uploadLocalResource(params));

        assertThat(failure).isInstanceOf(AppException.class);
        assertThat(failure.getSuppressed()).hasSize(1);
        assertThat(failure.getSuppressed()[0]).hasMessage("delete failed");
    }

    @Test
    void shouldRejectAvatarWithNonImageMime() {
        ResourceCreateParamsBO params = new ResourceCreateParamsBO(
                1L, 10L, "avatar.txt", "txt", 3L, "text/plain", "avatar", 1L);

        assertThatThrownBy(() -> resourceService.create(params))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("只能上传图片");

        verifyNoInteractions(storageFactory, resourceMapper);
    }

    @Test
    void readLocalResource_notFound_throws() {
        when(resourceMapper.selectCount(any())).thenReturn(0L);
        LocalFileReadParamsBO params = localFileUrlSigner.sign("missing.png");

        assertThatThrownBy(() -> resourceService.readLocalResource(params))
                .isInstanceOf(AppException.class);

    }

    @Test
    void readLocalResource_validSignature_returnsResource() {
        when(resourceMapper.selectCount(any())).thenReturn(1L);
        when(storageFactory.get(StorageType.LOCAL)).thenReturn(storageService);
        org.springframework.core.io.Resource mockResource = mock(org.springframework.core.io.Resource.class);
        when(storageService.getResource("file/1.png")).thenReturn(mockResource);
        LocalFileReadParamsBO params = localFileUrlSigner.sign("file/1.png");

        org.springframework.core.io.Resource result = resourceService.readLocalResource(params);

        assertThat(result).isEqualTo(mockResource);
    }

    @Test
    void readLocalResource_invalidSignature_rejectsBeforeLookup() {
        LocalFileReadParamsBO params = new LocalFileReadParamsBO("file/1.png", 1L, "0".repeat(64));

        assertThatThrownBy(() -> resourceService.readLocalResource(params))
                .isInstanceOf(AppException.class);

        verifyNoInteractions(resourceMapper, storageFactory);
    }
}

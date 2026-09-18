package online.longlian.app.service.resource;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.impl.LocalStorageService;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalFileIngressTest {

    private static final LocalFileReadParamsBO SIGNED =
            new LocalFileReadParamsBO("file/1.png", 1_000L, "a".repeat(64));

    @Mock
    private ResourceService resourceService;
    @Mock
    private LocalStorageService localStorageService;
    @Mock
    private LocalFileUrlSigner signer;
    @InjectMocks
    private LocalFileIngress ingress;

    @Test
    void upload_valid_writesAndActivates() {
        Resource pending = pendingLocal(3L, "image/png");
        when(resourceService.loadPending("file/1.png")).thenReturn(pending);

        ingress.upload(SIGNED, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3L);

        verify(signer).verifyUpload(SIGNED);
        verify(localStorageService).upload(argThat((LocalFileWriteParamsBO writeParams) ->
                writeParams.getStorageKey().equals("file/1.png")
                        && writeParams.getExpectedSize().equals(3L)
                        && writeParams.getExpectedMimeType().equals("image/png")));
        verify(resourceService).loadPending("file/1.png");
    }

    @Test
    void upload_sizeMismatch_throwsBeforeWrite() {
        when(resourceService.loadPending("file/1.png")).thenReturn(pendingLocal(999L, null));

        assertThatThrownBy(() -> ingress.upload(SIGNED, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件大小不匹配");

        verifyNoInteractions(localStorageService);
    }

    @Test
    void upload_chunkedContent_skipsLengthCheck() {
        when(resourceService.loadPending("file/1.png")).thenReturn(pendingLocal(3L, "image/png"));

        ingress.upload(SIGNED, new ByteArrayInputStream(new byte[]{1, 2, 3}), -1L);

        verify(localStorageService).upload(any(LocalFileWriteParamsBO.class));
        verify(resourceService).loadPending("file/1.png");
    }

    @Test
    void upload_nonLocalPending_rejectsWithoutWrite() {
        when(resourceService.loadPending("file/1.png")).thenReturn(
                Resource.builder().id(1L).fileSize(3L).storageType(StorageType.OSS).build());

        assertThatThrownBy(() -> ingress.upload(SIGNED, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("无权上传或文件已完成上传");

        verifyNoInteractions(localStorageService);
    }

    @Test
    void upload_invalidSignature_rejectsBeforeLookup() {
        doThrow(new AppException(ResultCode.UNAUTHORIZED_OPERATION, "文件链接无效或已过期"))
                .when(signer).verifyUpload(SIGNED);

        assertThatThrownBy(() -> ingress.upload(SIGNED, new ByteArrayInputStream(new byte[]{1, 2, 3}), 3L))
                .isInstanceOf(AppException.class);

        verifyNoInteractions(resourceService, localStorageService);
    }

    @Test
    void read_valid_returnsResource() {
        org.springframework.core.io.Resource mockResource = mock(org.springframework.core.io.Resource.class);
        when(resourceService.loadActivated("file/1.png")).thenReturn(pendingLocal(1L, "image/png"));
        when(localStorageService.getResource("file/1.png")).thenReturn(mockResource);

        assertThat(ingress.read(SIGNED)).isSameAs(mockResource);

    }

    @Test
    void read_nonLocalActivated_rejectsWithoutRead() {
        when(resourceService.loadActivated("file/1.png")).thenReturn(
                Resource.builder().id(1L).storageType(StorageType.OSS).build());

        assertThatThrownBy(() -> ingress.read(SIGNED)).isInstanceOf(AppException.class);

        verifyNoInteractions(localStorageService);
    }

    @Test
    void read_invalidSignature_rejectsBeforeLookup() {
        StorageProperties properties = new StorageProperties();
        properties.setPresignedUrlTtlSeconds(60);
        LocalFileIngress ingressWithRealSigner = new LocalFileIngress(
                resourceService,
                localStorageService,
                new LocalFileUrlSigner("a".repeat(32), properties, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        );

        assertThatThrownBy(() -> ingressWithRealSigner.read(SIGNED))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件链接无效或已过期");

        verifyNoInteractions(resourceService, localStorageService);
    }

    private static Resource pendingLocal(long fileSize, String mime) {
        return Resource.builder()
                .id(1L)
                .fileSize(fileSize)
                .fileMime(mime)
                .storageType(StorageType.LOCAL)
                .build();
    }
}

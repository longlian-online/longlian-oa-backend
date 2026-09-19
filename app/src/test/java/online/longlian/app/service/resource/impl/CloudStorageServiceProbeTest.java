package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CloudStorageServiceProbeTest {

    private static final ResourceProbeParamsBO PROBE = new ResourceProbeParamsBO("resource.png", 3L, "image/png");
    private static final long PRESIGNED_URL_TTL_SECONDS = 120;
    @Test
    void shouldGenerateCloudUploadAndReadUrls() throws Exception {
        URL signedUrl = new URL("https://cos.example/resource.png");
        for (CloudStorage cloud : cloudStorageServices()) {
            when(cloud.client().generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(signedUrl);

            var upload = cloud.storage().generatePresignedUploadUrl(new PresignedUploadUrlParamsBO("resource.png"));
            assertThat(upload.getUploadUrl()).isEqualTo(signedUrl.toString());
            assertThat(upload.getKey()).isEqualTo("resource.png");
            assertThat(cloud.storage().getResourceReadUrl("resource.png")).isEqualTo(signedUrl.toString());
            assertThat(cloud.storage().getResourceReadUrls(List.of("first.png", "second.png")))
                    .containsEntry("first.png", signedUrl.toString())
                    .containsEntry("second.png", signedUrl.toString());
        }
    }

    @Test
    void shouldUseConfiguredPresignedUrlTtlForUploads() throws Exception {
        Instant before = Instant.now().truncatedTo(ChronoUnit.MILLIS);
        URL signedUrl = new URL("https://cdn.example/resource.png");
        for (CloudStorage cloud : cloudStorageServices()) {
            when(cloud.client().generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(signedUrl);

            cloud.storage().generatePresignedUploadUrl(new PresignedUploadUrlParamsBO("resource.png"));

            ArgumentCaptor<GeneratePresignedUrlRequest> request = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
            verify(cloud.client(), times(1)).generatePresignedUrl(request.capture());
            assertThat(request.getValue().getExpiration().toInstant())
                    .isBetween(before.plusSeconds(PRESIGNED_URL_TTL_SECONDS), Instant.now().plusSeconds(PRESIGNED_URL_TTL_SECONDS));
        }
    }

    @Test
    void shouldShutdownCloudClients() throws Exception {
        for (CloudStorage cloud : cloudStorageServices()) {
            ((DisposableBean) cloud.storage()).destroy();

            verify(cloud.client()).shutdown();
        }
    }

    @Test
    void shouldDeleteExactCloudObject() {
        for (CloudStorage cloud : cloudStorageServices()) {
            cloud.storage().delete("deprecated/avatar/1.png");

            verify(cloud.client()).deleteObject(cloud.bucket(), "deprecated/avatar/1.png");
        }
    }


    @Test
    void shouldAcceptCloudObjectsWithExpectedSize() {
        for (CloudStorage cloud : cloudStorageServices()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(3L);
            when(cloud.client().getObjectMetadata(cloud.bucket(), PROBE.storageKey())).thenReturn(metadata);

            cloud.storage().probe(PROBE);
        }
    }

    @Test
    void shouldRejectCloudObjectsWithUnexpectedSize() {
        for (CloudStorage cloud : cloudStorageServices()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(2L);
            when(cloud.client().getObjectMetadata(cloud.bucket(), PROBE.storageKey())).thenReturn(metadata);

            assertThatThrownBy(() -> cloud.storage().probe(PROBE))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("文件未完成上传或内容不匹配");
        }
    }

    @Test
    void shouldRejectCloudProbeWhenObjectStoreFails() {
        for (CloudStorage cloud : cloudStorageServices()) {
            when(cloud.client().getObjectMetadata(cloud.bucket(), PROBE.storageKey()))
                    .thenThrow(new IllegalStateException("object store unavailable"));

            assertThatThrownBy(() -> cloud.storage().probe(PROBE))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("文件未完成上传或内容不匹配");
        }
    }

    private List<CloudStorage> cloudStorageServices() {
        StorageProperties properties = new StorageProperties();
        properties.setPresignedUrlTtlSeconds(PRESIGNED_URL_TTL_SECONDS);

        StorageProperties.CosConfig cosConfig = storageConfig(new StorageProperties.CosConfig());
        properties.setCos(cosConfig);
        CosStorageService cosStorage = new CosStorageService(properties);
        COSClient cosClient = mock(COSClient.class);
        ReflectionTestUtils.setField(cosStorage, "cosClient", cosClient);

        StorageProperties.OssConfig ossConfig = storageConfig(new StorageProperties.OssConfig());
        properties.setOss(ossConfig);
        OssStorageService ossStorage = new OssStorageService(properties);
        COSClient ossClient = mock(COSClient.class);
        ReflectionTestUtils.setField(ossStorage, "cosClient", ossClient);

        return List.of(
                new CloudStorage(cosStorage, cosClient, cosConfig.getBucket()),
                new CloudStorage(ossStorage, ossClient, ossConfig.getBucket())
        );
    }

    private <T extends StorageProperties.CosConfig> T storageConfig(T config) {
        config.setBucket("cos-bucket");
        config.setRegion("ap-guangzhou");
        config.setSecretId("test-secret-id");
        config.setSecretKey("test-secret-key");
        return config;
    }

    private StorageProperties.OssConfig storageConfig(StorageProperties.OssConfig config) {
        config.setBucket("oss-bucket");
        config.setRegion("ap-guangzhou");
        config.setSecretId("test-secret-id");
        config.setSecretKey("test-secret-key");
        return config;
    }

    private record CloudStorage(StorageService storage, COSClient client, String bucket) {
    }
}

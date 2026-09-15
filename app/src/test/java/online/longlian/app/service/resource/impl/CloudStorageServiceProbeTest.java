package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CloudStorageServiceProbeTest {

    private static final ResourceProbeParamsBO PROBE = new ResourceProbeParamsBO("resource.png", 3L, "image/png");

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
        COSClient cosClient = mock(COSClient.class);
        CosStorageService cosStorage = mock(CosStorageService.class, CALLS_REAL_METHODS);
        StorageProperties.CosConfig cosConfig = new StorageProperties.CosConfig();
        cosConfig.setBucket("cos-bucket");
        ReflectionTestUtils.setField(cosStorage, "cosClient", cosClient);
        ReflectionTestUtils.setField(cosStorage, "cosConfig", cosConfig);

        COSClient ossClient = mock(COSClient.class);
        OssStorageService ossStorage = mock(OssStorageService.class, CALLS_REAL_METHODS);
        StorageProperties.OssConfig ossConfig = new StorageProperties.OssConfig();
        ossConfig.setBucket("oss-bucket");
        ReflectionTestUtils.setField(ossStorage, "cosClient", ossClient);
        ReflectionTestUtils.setField(ossStorage, "ossConfig", ossConfig);

        return List.of(
                new CloudStorage(cosStorage, cosClient, cosConfig.getBucket()),
                new CloudStorage(ossStorage, ossClient, ossConfig.getBucket())
        );
    }

    private record CloudStorage(StorageService storage, COSClient client, String bucket) {
    }
}

package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import online.longlian.app.common.properties.StorageProperties;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class OssStorageServiceTest {
    @Test
    void shouldDeleteExactObjectKeyFromConfiguredBucket() {
        COSClient cosClient = mock(COSClient.class);
        StorageProperties.OssConfig config = new StorageProperties.OssConfig();
        config.setBucket("resource-bucket");
        OssStorageService storageService = new OssStorageService(cosClient, config);

        storageService.delete("cover/obsolete.jpg");

        verify(cosClient).deleteObject("resource-bucket", "cover/obsolete.jpg");
    }
}

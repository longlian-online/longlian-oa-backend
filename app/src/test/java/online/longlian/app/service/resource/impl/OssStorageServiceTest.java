package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import online.longlian.app.common.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class OssStorageServiceTest {
    @Test
    public void shouldDeleteExactObjectKeyFromConfiguredBucket() {
        COSClient cosClient = mock(COSClient.class);
        StorageProperties.OssConfig config = new StorageProperties.OssConfig();
        config.setBucket("resource-bucket");
        OssStorageService storageService = new OssStorageService(cosClient, config);

        storageService.delete("cover/obsolete.jpg");

        ArgumentCaptor<String> bucket = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
        verify(cosClient).deleteObject(bucket.capture(), storageKey.capture());

        assertThat(bucket.getValue()).isEqualTo("resource-bucket");
        assertThat(storageKey.getValue()).isEqualTo("cover/obsolete.jpg");
    }
}

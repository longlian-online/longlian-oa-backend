package online.longlian.app.service.resource.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.net.URL;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OssStorageServiceTest {
    @Test
    public void shouldDeleteExactObjectKeyFromConfiguredBucket() {
        COSClient cosClient = mock(COSClient.class);
        OssStorageService storageService = new OssStorageService(cosClient, config());

        storageService.delete("cover/obsolete.jpg");

        ArgumentCaptor<String> bucket = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> storageKey = ArgumentCaptor.forClass(String.class);
        verify(cosClient).deleteObject(bucket.capture(), storageKey.capture());
        assertThat(bucket.getValue()).isEqualTo("resource-bucket");
        assertThat(storageKey.getValue()).isEqualTo("cover/obsolete.jpg");
    }

    @Test
    public void shouldGenerateReadAndUploadUrlsAndReleaseClient() throws Exception {
        COSClient cosClient = mock(COSClient.class);
        when(cosClient.generatePresignedUrl(any(GeneratePresignedUrlRequest.class)))
                .thenReturn(new URL("https://resource-bucket.example.test/presigned"));
        OssStorageService storageService = new OssStorageService(cosClient, config());

        assertThat(storageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO("cover/new.jpg"))
                .getUploadUrl()).isEqualTo("https://resource-bucket.example.test/presigned");
        assertThat(storageService.getResourceReadUrl("cover/new.jpg"))
                .isEqualTo("https://resource-bucket.example.test/presigned");
        assertThat(storageService.getResourceReadUrls(List.of("cover/new.jpg")))
                .isEqualTo(Map.of("cover/new.jpg", "https://resource-bucket.example.test/presigned"));
        storageService.destroy();

        verify(cosClient, times(3)).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
        verify(cosClient).shutdown();
    }

    private StorageProperties.OssConfig config() {
        StorageProperties.OssConfig config = new StorageProperties.OssConfig();
        config.setBucket("resource-bucket");
        return config;
    }
}

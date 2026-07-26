package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.app.service.resource.StorageServiceFactory;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NullStorageServiceTest {

    private final NullStorageService service = new NullStorageService();

    @Test
    void getStorageType_returnsNone() {
        assertThat(service.getStorageType()).isEqualTo(StorageType.NONE);
    }

    @Test
    void generatePresignedUploadUrl_returnsEmptyUrl() {
        PresignedUploadUrlParamsBO params = new PresignedUploadUrlParamsBO("test/file.png");
        PresignedUploadUrlResultBO result = service.generatePresignedUploadUrl(params);

        assertThat(result.getUploadUrl()).isEmpty();
        assertThat(result.getKey()).isEqualTo("test/file.png");
    }

    @Test
    void getResourceReadUrl_returnsEmpty() {
        assertThat(service.getResourceReadUrl("any/key.png")).isEmpty();
    }

    @Test
    void getResourceReadUrls_returnsEmptyMap() {
        Map<String, String> result = service.getResourceReadUrls(List.of("a.png", "b.png"));
        assertThat(result).isEmpty();
    }

    @Test
    void upload_throwsUnsupported() {
        assertThatThrownBy(() -> service.upload("key", new byte[]{}))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void getResource_throwsUnsupported() {
        assertThatThrownBy(() -> service.getResource("key"))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}

class StorageServiceFactoryTest {

    @Test
    void get_existingType_returnsService() {
        NullStorageService nullService = new NullStorageService();
        StorageServiceFactory factory = new StorageServiceFactory(List.of(nullService));

        StorageService result = factory.get(StorageType.NONE);

        assertThat(result).isEqualTo(nullService);
    }

    @Test
    void get_unknownType_throws() {
        StorageServiceFactory factory = new StorageServiceFactory(Collections.emptyList());

        assertThatThrownBy(() -> factory.get(StorageType.OSS))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未找到对应的存储策略");
    }
}

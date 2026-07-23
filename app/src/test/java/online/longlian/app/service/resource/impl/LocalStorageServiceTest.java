package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

    @Test
    void shouldBuildReadableLocalResourceUrl() {
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setBaseUrl("https://api.example.com");
        StorageProperties properties = new StorageProperties();
        properties.setLocal(localConfig);
        LocalStorageService storageService = new LocalStorageService(properties);

        assertThat(storageService.getResourceReadUrl("avatar/1.png"))
                .isEqualTo("https://api.example.com/common/file/local?key=avatar/1.png");
    }

    @Test
    void shouldStoreAndReadLocalResource(@TempDir Path directory) throws IOException {
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setDirectory(directory.toString());
        StorageProperties properties = new StorageProperties();
        properties.setLocal(localConfig);
        LocalStorageService storageService = new LocalStorageService(properties);

        storageService.upload("avatar/1.png", new byte[]{1, 2, 3});

        assertThat(storageService.getResource("avatar/1.png").getContentAsByteArray())
                .containsExactly(1, 2, 3);
    }
}

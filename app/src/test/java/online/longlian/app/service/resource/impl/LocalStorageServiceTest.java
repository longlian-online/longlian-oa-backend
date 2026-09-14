package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;

class LocalStorageServiceTest {

    @Test
    void shouldBuildReadableLocalResourceUrl() {
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setBaseUrl("https://api.example.com");
        StorageProperties properties = new StorageProperties();
        properties.setLocal(localConfig);
        LocalStorageService storageService = new LocalStorageService(properties, signer());

        assertThat(storageService.getResourceReadUrl("avatar/1.png"))
                .matches("https://api.example.com/common/file/local\\?key=avatar/1.png&expires=[0-9]+&signature=[0-9a-f]{64}");
    }

    @Test
    void shouldStoreAndReadLocalResource(@TempDir Path directory) throws IOException {
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setDirectory(directory.toString());
        StorageProperties properties = new StorageProperties();
        properties.setLocal(localConfig);
        LocalStorageService storageService = new LocalStorageService(properties, signer());

        storageService.upload("avatar/1.png", new byte[]{1, 2, 3});

        assertThat(storageService.getResource("avatar/1.png").getContentAsByteArray())
                .containsExactly(1, 2, 3);
    }

    private LocalFileUrlSigner signer() {
        return new LocalFileUrlSigner("test-local-signing-secret-32-bytes", 300, Clock.systemUTC());
    }
}

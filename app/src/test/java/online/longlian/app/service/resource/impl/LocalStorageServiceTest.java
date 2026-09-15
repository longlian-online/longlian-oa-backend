package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class LocalStorageServiceTest {
    @TempDir
    Path storageRoot;

    @Test
    public void shouldDeleteOnlyTheExactStorageKey() throws Exception {
        Path obsoleteFile = Files.createDirectories(storageRoot.resolve("avatar")).resolve("obsolete.png");
        Files.writeString(obsoleteFile, "obsolete");
        Path activeFile = Files.writeString(storageRoot.resolve("avatar/active.png"), "active");
        LocalStorageService storageService = new LocalStorageService(storageProperties());

        storageService.delete("avatar/obsolete.png");
        storageService.delete("avatar/obsolete.png");

        assertThat(obsoleteFile).doesNotExist();
        assertThat(activeFile).exists();
    }

    @Test
    public void shouldRejectStorageKeyOutsideConfiguredRoot() {
        LocalStorageService storageService = new LocalStorageService(storageProperties());

        assertThatThrownBy(() -> storageService.delete("../outside"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void shouldReturnReadAndUploadUrlsForConfiguredBaseUrl() {
        StorageProperties properties = storageProperties();
        properties.getLocal().setBaseUrl("https://files.example.test/");
        LocalStorageService storageService = new LocalStorageService(properties);

        assertThat(storageService.generatePresignedUploadUrl(new PresignedUploadUrlParamsBO("avatar/1.png"))
                .getUploadUrl()).isEqualTo("https://files.example.test/upload/local?key=avatar/1.png");
        assertThat(storageService.getResourceReadUrl("avatar/1.png"))
                .isEqualTo("https://files.example.test/avatar/1.png");
        assertThat(storageService.getResourceReadUrls(List.of("avatar/1.png")))
                .isEqualTo(Map.of("avatar/1.png", "https://files.example.test/avatar/1.png"));
    }

    @Test
    public void shouldRetainFailedLocalDeletionForCleanupRetry() throws Exception {
        Path nonEmptyDirectory = Files.createDirectories(storageRoot.resolve("blocked"));
        Files.writeString(nonEmptyDirectory.resolve("child"), "content");
        LocalStorageService storageService = new LocalStorageService(storageProperties());

        assertThatThrownBy(() -> storageService.delete("blocked"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blocked");
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setBasePath(storageRoot.toString());
        properties.setLocal(localConfig);
        return properties;
    }
}

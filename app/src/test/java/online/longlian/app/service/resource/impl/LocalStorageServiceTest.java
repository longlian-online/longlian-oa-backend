package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {
    @TempDir
    Path storageRoot;

    @Test
    void shouldDeleteOnlyTheExactStorageKey() throws Exception {
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
    void shouldRejectStorageKeyOutsideConfiguredRoot() {
        LocalStorageService storageService = new LocalStorageService(storageProperties());

        assertThatThrownBy(() -> storageService.delete("../outside"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private StorageProperties storageProperties() {
        StorageProperties properties = new StorageProperties();
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setBasePath(storageRoot.toString());
        properties.setLocal(localConfig);
        return properties;
    }
}

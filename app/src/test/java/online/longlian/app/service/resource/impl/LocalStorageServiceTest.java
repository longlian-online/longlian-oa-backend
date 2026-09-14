package online.longlian.app.service.resource.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
        LocalStorageService storageService = storageService(directory);

        storageService.upload("avatar/1.png", new byte[]{1, 2, 3});

        assertThat(storageService.getResource("avatar/1.png").getContentAsByteArray())
                .containsExactly(1, 2, 3);
    }

    @Test
    void shouldStreamUploadWhenSizeMatches(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] content = {1, 2, 3};

        storageService.upload(writeParams("task/1.bin", content, 3L, "application/octet-stream"));

        assertThat(storageService.getResource("task/1.bin").getContentAsByteArray()).containsExactly(content);
    }

    @Test
    void shouldRejectSizeMismatchAndRemoveTemporaryFile(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);

        assertThatThrownBy(() -> storageService.upload(
                writeParams("task/1.bin", new byte[]{1, 2, 3}, 2L, "application/octet-stream")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件大小不匹配");

        assertThat(directory.resolve("task/1.bin")).doesNotExist();
        try (java.util.stream.Stream<Path> files = Files.list(directory.resolve("task"))) {
            assertThat(files).isEmpty();
        }
    }

    @Test
    void shouldRejectOverwriteAndKeepOriginalContent(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        storageService.upload("task/1.bin", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> storageService.upload("task/1.bin", new byte[]{4, 5, 6}))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已完成上传");

        assertThat(storageService.getResource("task/1.bin").getContentAsByteArray())
                .containsExactly(1, 2, 3);
    }

    @Test
    void shouldAcceptImageWhoseContentMatchesMimeType(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] png = createPng();

        storageService.upload(writeParams("avatar/1.png", png, png.length, "image/png"));

        assertThat(storageService.getResource("avatar/1.png").getContentAsByteArray()).isEqualTo(png);
    }

    @Test
    void shouldRejectFakeImageContent(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);
        byte[] content = "not-an-image".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        assertThatThrownBy(() -> storageService.upload(
                writeParams("avatar/1.png", content, content.length, "image/png")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不是有效图片");

        assertThat(directory.resolve("avatar/1.png")).doesNotExist();
    }

    @Test
    void shouldRejectImageWhoseContentDoesNotMatchMimeType(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] png = createPng();

        assertThatThrownBy(() -> storageService.upload(
                writeParams("avatar/1.jpg", png, png.length, "image/jpeg")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("MIME 类型不匹配");
    }

    @Test
    void shouldDeleteStoredFile(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);
        storageService.upload("task/1.bin", new byte[]{1});

        storageService.delete("task/1.bin");

        assertThat(directory.resolve("task/1.bin")).doesNotExist();
    }

    private LocalStorageService storageService(Path directory) {
        StorageProperties.LocalConfig localConfig = new StorageProperties.LocalConfig();
        localConfig.setDirectory(directory.toString());
        StorageProperties properties = new StorageProperties();
        properties.setLocal(localConfig);
        return new LocalStorageService(properties, signer());
    }

    private LocalFileWriteParamsBO writeParams(String key, byte[] content, long size, String mimeType) {
        return LocalFileWriteParamsBO.builder()
                .storageKey(key)
                .content(new ByteArrayInputStream(content))
                .expectedSize(size)
                .expectedMimeType(mimeType)
                .build();
    }

    private byte[] createPng() throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);
        return output.toByteArray();
    }

    private LocalFileUrlSigner signer() {
        return new LocalFileUrlSigner("test-local-signing-secret-32-bytes", 300, Clock.systemUTC());
    }
}

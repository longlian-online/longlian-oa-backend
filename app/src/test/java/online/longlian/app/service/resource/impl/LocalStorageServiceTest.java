package online.longlian.app.service.resource.impl;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.ResourceProbeParamsBO;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import online.longlian.common.enumeration.StorageType;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @Test
    void shouldIdentifyLocalStorageType(@TempDir Path directory) {
        assertThat(storageService(directory).getStorageType()).isEqualTo(StorageType.LOCAL);
    }

    @Test
    void shouldBuildLocalUploadAndBatchReadUrls(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);

        assertThat(storageService.generatePresignedUploadUrl(
                new PresignedUploadUrlParamsBO("avatar/1.png")))
                .satisfies(result -> {
                    assertThat(result.getKey()).isEqualTo("avatar/1.png");
                    assertThat(result.getUploadUrl())
                            .contains("avatar/1.png")
                            .contains("expires=")
                            .matches(".*signature=[0-9a-f]{64}.*");
                });
        Map<String, String> urls = storageService.getResourceReadUrls(List.of("avatar/1.png", "cover/2.png"));
        assertThat(urls).containsOnlyKeys("avatar/1.png", "cover/2.png");
        assertThat(urls.values()).allMatch(url -> url.contains("expires=") && url.contains("signature="));
    }

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

        storageService.upload(writeParams("avatar/1.png", new byte[]{1, 2, 3}, 3L, null));

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
    void shouldRejectContentShorterThanDeclaredSize(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);

        assertThatThrownBy(() -> storageService.upload(
                writeParams("task/1.bin", new byte[]{1, 2}, 3L, "application/octet-stream")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("文件大小不匹配");
    }

    @Test
    void shouldReportInputStreamReadFailure(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);
        InputStream failingContent = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("read failed");
            }
        };
        LocalFileWriteParamsBO params = LocalFileWriteParamsBO.builder()
                .storageKey("task/1.bin")
                .content(failingContent)
                .expectedSize(1L)
                .expectedMimeType("application/octet-stream")
                .build();

        assertThatThrownBy(() -> storageService.upload(params))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本地文件写入失败");
    }

    @Test
    void shouldRejectOverwriteAndKeepOriginalContent(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        storageService.upload(writeParams("task/1.bin", new byte[]{1, 2, 3}, 3L, null));

        assertThatThrownBy(() -> storageService.upload(
                writeParams("task/1.bin", new byte[]{4, 5, 6}, 3L, null)))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("已完成上传");

        assertThat(storageService.getResource("task/1.bin").getContentAsByteArray())
                .containsExactly(1, 2, 3);
    }

    @Test
    void shouldRejectConcurrentWriteAndKeepFirstContent(@TempDir Path directory) throws Exception {
        LocalStorageService storageService = storageService(directory);
        CountDownLatch opened = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        InputStream delayed = new ByteArrayInputStream(new byte[]{1}) {
            @Override
            public int read(byte[] buffer, int offset, int length) {
                opened.countDown();
                try {
                    resume.await(10, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(e);
                }
                return super.read(buffer, offset, length);
            }
        };
        try (ExecutorService executor = Executors.newSingleThreadExecutor()) {
            var first = executor.submit(() -> storageService.upload(LocalFileWriteParamsBO.builder()
                    .storageKey("task/1.bin").content(delayed).expectedSize(1L).build()));
            assertThat(opened.await(10, TimeUnit.SECONDS)).isTrue();

            assertThatThrownBy(() -> storageService.upload(writeParams("task/1.bin", new byte[]{2}, 1L, null)))
                    .isInstanceOf(AppException.class);
            resume.countDown();
            first.get(10, TimeUnit.SECONDS);
        }

        assertThat(Files.readAllBytes(directory.resolve("task/1.bin"))).containsExactly(1);
    }

    @Test
    void shouldProbeStoredFileBeforeBinding(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] png = createPng();
        storageService.upload(writeParams("avatar/1.png", png, png.length, "image/png"));

        storageService.probe(new ResourceProbeParamsBO("avatar/1.png", (long) png.length, "image/png"));

        assertThatThrownBy(() -> storageService.probe(
                new ResourceProbeParamsBO("avatar/1.png", (long) png.length + 1, "image/png")))
                .isInstanceOf(AppException.class);
        assertThatThrownBy(() -> storageService.probe(
                new ResourceProbeParamsBO("missing.png", 1L, "image/png")))
                .isInstanceOf(AppException.class);
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
    void shouldRejectTruncatedImageContent(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] truncatedPng = Arrays.copyOf(createPng(), 24);

        assertThatThrownBy(() -> storageService.upload(
                writeParams("avatar/1.png", truncatedPng, truncatedPng.length, "image/png")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("不是有效图片");
    }

    @Test
    void shouldRejectImageWithExcessivePixelCount(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] oversizedPngHeader = createPngHeader(5_001, 5_001);

        assertThatThrownBy(() -> storageService.upload(writeParams(
                "avatar/1.png", oversizedPngHeader, oversizedPngHeader.length, "image/png")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("像素过大");
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
    void shouldAcceptJpegAndGifContent(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] jpeg = createImage("jpeg");
        byte[] gif = createImage("gif");

        storageService.upload(writeParams("avatar/1.jpg", jpeg, jpeg.length, "image/jpeg"));
        storageService.upload(writeParams("avatar/2.gif", gif, gif.length, "image/gif"));

        assertThat(storageService.getResource("avatar/1.jpg").contentLength()).isEqualTo(jpeg.length);
        assertThat(storageService.getResource("avatar/2.gif").contentLength()).isEqualTo(gif.length);
    }

    @Test
    void shouldRejectUnsupportedImageMimeType(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        byte[] png = createPng();

        assertThatThrownBy(() -> storageService.upload(
                writeParams("avatar/1.webp", png, png.length, "image/webp")))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("MIME 类型不匹配");
    }

    @Test
    void shouldRejectPathTraversal(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);

        assertThatThrownBy(() -> storageService.upload(
                writeParams("../outside.bin", new byte[]{1}, 1L, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("非法文件 key");
    }

    @Test
    void shouldFailWhenStoredResourceDoesNotExist(@TempDir Path directory) {
        assertThatThrownBy(() -> storageService(directory).getResource("missing.bin"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本地文件不存在");
    }

    @Test
    void shouldReportDeleteFailureForNonEmptyDirectory(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        Files.createDirectories(directory.resolve("task/folder"));
        Files.write(directory.resolve("task/folder/file.bin"), new byte[]{1});

        assertThatThrownBy(() -> storageService.delete("task/folder"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("本地文件删除失败");
    }

    @Test
    void shouldDeleteStoredFile(@TempDir Path directory) {
        LocalStorageService storageService = storageService(directory);
        storageService.upload(writeParams("task/1.bin", new byte[]{1}, 1L, null));

        storageService.delete("task/1.bin");

        assertThat(directory.resolve("task/1.bin")).doesNotExist();
    }

    @Test
    void shouldTreatMissingStoredFileAsDeleted(@TempDir Path directory) {
        storageService(directory).delete("task/missing.bin");
    }

    @Test
    void shouldRejectProbeWhenFileSizeCannotBeRead(@TempDir Path directory) throws IOException {
        LocalStorageService storageService = storageService(directory);
        Path target = directory.toAbsolutePath().normalize().resolve("avatar/1.png");

        try (MockedStatic<Files> files = Mockito.mockStatic(Files.class, Mockito.CALLS_REAL_METHODS)) {
            files.when(() -> Files.isRegularFile(target)).thenReturn(true);
            files.when(() -> Files.size(target)).thenThrow(new IOException("read failed"));

            assertThatThrownBy(() -> storageService.probe(new ResourceProbeParamsBO("avatar/1.png", 1L, null)))
                    .isInstanceOf(AppException.class)
                    .hasMessageContaining("文件未完成上传或内容不匹配");
        }
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
        return createImage("png");
    }

    private byte[] createImage(String format) throws IOException {
        BufferedImage image = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }

    private byte[] createPngHeader(int width, int height) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataOutputStream data = new DataOutputStream(output)) {
            data.write(new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            data.writeInt(13);
            byte[] chunkType = {'I', 'H', 'D', 'R'};
            ByteArrayOutputStream chunkOutput = new ByteArrayOutputStream();
            try (DataOutputStream chunk = new DataOutputStream(chunkOutput)) {
                chunk.write(chunkType);
                chunk.writeInt(width);
                chunk.writeInt(height);
                chunk.write(new byte[]{8, 2, 0, 0, 0});
            }
            byte[] chunkBytes = chunkOutput.toByteArray();
            data.write(chunkBytes);
            CRC32 crc = new CRC32();
            crc.update(chunkBytes);
            data.writeInt((int) crc.getValue());
        }
        return output.toByteArray();
    }

    private LocalFileUrlSigner signer() {
        return new LocalFileUrlSigner("test-local-signing-secret-32-bytes", 300, Clock.systemUTC());
    }
}

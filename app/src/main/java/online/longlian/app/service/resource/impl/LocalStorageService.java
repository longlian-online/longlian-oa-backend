package online.longlian.app.service.resource.impl;

import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.LocalFileUrlSigner;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalStorageService implements StorageService {
    private static final int COPY_BUFFER_SIZE = 8192;
    private static final long MAX_IMAGE_PIXELS = 25_000_000L;
    private static final String IMAGE_MIME_PREFIX = "image/";
    private static final String JPEG_MIME_TYPE = "image/jpeg";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final String GIF_MIME_TYPE = "image/gif";

    private final StorageProperties.LocalConfig localConfig;
    private final LocalFileUrlSigner signer;

    LocalStorageService(StorageProperties storageProperties, LocalFileUrlSigner signer) {
        localConfig = storageProperties.getLocal();
        this.signer = signer;
    }

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        String key = params.getKey();
        String uploadUrl = buildLocalResourceUrl(key);
        return new PresignedUploadUrlResultBO(uploadUrl, key);
    }

    @Override
    public String getResourceReadUrl(String key) {
        LocalFileReadParamsBO signed = signer.sign(key);
        return buildLocalResourceUrl(key) + "&expires=" + signed.expires() + "&signature=" + signed.signature();
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }

    @Override
    public void upload(LocalFileWriteParamsBO params) {
        Path target = resolveKey(params.getStorageKey());
        Path temporaryFile = null;
        try {
            Files.createDirectories(target.getParent());
            if (Files.exists(target)) {
                throw new AppException(ResultCode.OPERATION_FAIL, "文件已完成上传");
            }
            temporaryFile = Files.createTempFile(target.getParent(), ".upload-", ".tmp");
            writeContent(params.getContent(), temporaryFile, params.getExpectedSize());
            validateImage(temporaryFile, params.getExpectedMimeType());
            moveIntoPlace(temporaryFile, target);
            temporaryFile = null;
        } catch (IOException e) {
            throw new IllegalStateException("本地文件写入失败", e);
        } finally {
            deleteTemporaryFile(temporaryFile);
        }
    }

    @Override
    public void delete(String key) {
        try {
            Files.deleteIfExists(resolveKey(key));
        } catch (IOException e) {
            throw new IllegalStateException("本地文件删除失败", e);
        }
    }

    @Override
    public Resource getResource(String key) {
        Path target = resolveKey(key);
        if (!Files.isRegularFile(target)) {
            throw new IllegalStateException("本地文件不存在");
        }
        return new FileSystemResource(target);
    }

    private String buildLocalResourceUrl(String key) {
        String baseUrl = localConfig.getBaseUrl() == null ? "" : localConfig.getBaseUrl().replaceAll("/$", "");
        return baseUrl + "/common/file/local?key=" + UriUtils.encodeQueryParam(key, StandardCharsets.UTF_8);
    }

    private Path resolveKey(String key) {
        Path root = Path.of(localConfig.getDirectory() == null || localConfig.getDirectory().isBlank()
                        ? System.getProperty("java.io.tmpdir") + "/longlian-oa"
                        : localConfig.getDirectory())
                .toAbsolutePath()
                .normalize();
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) {
            throw new IllegalArgumentException("非法文件 key");
        }
        return target;
    }

    private void writeContent(InputStream content, Path temporaryFile, long expectedSize) throws IOException {
        long actualSize = 0;
        byte[] buffer = new byte[COPY_BUFFER_SIZE];
        try (content; OutputStream output = Files.newOutputStream(temporaryFile,
                StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)) {
            int read;
            while ((read = content.read(buffer)) != -1) {
                actualSize += read;
                if (actualSize > expectedSize) {
                    throw new AppException(ResultCode.PARAM_ERROR, "文件大小不匹配");
                }
                output.write(buffer, 0, read);
            }
        }
        if (actualSize != expectedSize) {
            throw new AppException(ResultCode.PARAM_ERROR, "文件大小不匹配");
        }
    }

    private void validateImage(Path file, String expectedMimeType) {
        if (expectedMimeType == null || !expectedMimeType.startsWith(IMAGE_MIME_PREFIX)) {
            return;
        }
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(file.toFile())) {
            // JDK 默认提供文件输入流 SPI；仅在运行时 SPI 被裁剪时才会返回 null。
            if (imageInput == null) { // skipcq: TCV-001
                throw invalidImage(); // skipcq: TCV-001
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInput, true, true);
                validateImageMetadata(reader, expectedMimeType);
                reader.read(0);
            } finally {
                reader.dispose();
            }
        } catch (AppException e) {
            throw e;
        } catch (IOException e) {
            throw invalidImage();
        }
    }

    private void validateImageMetadata(ImageReader reader, String expectedMimeType) throws IOException {
        int width = reader.getWidth(0);
        int height = reader.getHeight(0);
        if (width <= 0 || height <= 0 || (long) width * height > MAX_IMAGE_PIXELS) {
            throw new AppException(ResultCode.PARAM_ERROR, "图片尺寸无效或像素过大");
        }
        String formatName = reader.getFormatName().toLowerCase(Locale.ROOT);
        if (!matchesMimeType(formatName, expectedMimeType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ResultCode.PARAM_ERROR, "图片内容与 MIME 类型不匹配");
        }
    }

    private boolean matchesMimeType(String formatName, String mimeType) {
        return switch (mimeType) {
            case JPEG_MIME_TYPE -> "jpeg".equals(formatName) || "jpg".equals(formatName);
            case PNG_MIME_TYPE -> "png".equals(formatName);
            case GIF_MIME_TYPE -> "gif".equals(formatName);
            default -> false;
        };
    }

    private void moveIntoPlace(Path temporaryFile, Path target) throws IOException {
        try {
            Files.move(temporaryFile, target, StandardCopyOption.ATOMIC_MOVE);
        // 是否支持原子移动由底层文件系统决定，CI 无法稳定构造不支持场景。
        } catch (AtomicMoveNotSupportedException e) { // skipcq: TCV-001
            Files.move(temporaryFile, target); // skipcq: TCV-001
        }
    }

    private void deleteTemporaryFile(Path temporaryFile) {
        if (temporaryFile == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporaryFile);
        } catch (IOException e) { // skipcq: TCV-001
            // 上传失败时优先保留原始异常，临时文件由运维清理策略兜底。
            log.warn("本地上传临时文件清理失败 | path={} | type={}", // skipcq: TCV-001
                    temporaryFile, e.getClass().getSimpleName()); // skipcq: TCV-001
        }
    }

    private AppException invalidImage() {
        return new AppException(ResultCode.PARAM_ERROR, "文件内容不是有效图片");
    }
}

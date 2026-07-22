package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LocalStorageService implements StorageService {

    private final StorageProperties.LocalConfig localConfig;

    LocalStorageService(StorageProperties storageProperties) {
        localConfig = storageProperties.getLocal();
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
        return buildLocalResourceUrl(key);
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }

    @Override
    public void upload(String key, byte[] content) {
        Path target = resolveKey(key);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException e) {
            throw new IllegalStateException("本地文件写入失败", e);
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
}

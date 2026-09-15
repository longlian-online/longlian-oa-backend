package online.longlian.app.service.resource.impl;

import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;

import java.io.IOException;
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
    public void delete(String key) {
        Path root = Path.of(localConfig.getBasePath()).toAbsolutePath().normalize();
        Path file = root.resolve(key).normalize();
        if (!file.startsWith(root)) {
            throw new IllegalArgumentException("非法本地存储 key");
        }
        try {
            Files.deleteIfExists(file);
        } catch (IOException exception) {
            throw new IllegalStateException("删除本地存储文件失败: " + key, exception);
        }
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        String key = params.getKey();
        String uploadUrl = localConfig.getBaseUrl().replaceAll("/+$", "") + "/upload/local?key=" + key;
        return new PresignedUploadUrlResultBO(uploadUrl, key);
    }

    @Override
    public String getResourceReadUrl(String key) {
        return localConfig.getBaseUrl().replaceAll("/+$", "") + "/" + key;
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return keys.stream().collect(Collectors.toMap(key -> key, this::getResourceReadUrl));
    }
}
package online.longlian.app.service.resource.impl;

import lombok.AllArgsConstructor;
import online.longlian.app.common.properties.StorageProperties;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
        String uploadUrl = localConfig.getBaseUrl() + "/upload/local?key=" + key;
        return new PresignedUploadUrlResultBO(uploadUrl, key);
    }

    @Override
    public String getResourceReadUrl(String key) {
        return "";
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return Map.of();
    }
}
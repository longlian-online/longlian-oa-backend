package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.bo.common.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.common.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class NullStorageService implements StorageService {
    @Override
    public StorageType getStorageType() {
        return StorageType.NONE;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        return new PresignedUploadUrlResultBO("", params.getKey());
    }

    @Override
    public String getResourceReadUrl(String key) {
        return "";
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        return Collections.emptyMap();
    }
}
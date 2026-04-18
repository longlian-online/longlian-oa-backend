package online.longlian.app.service.resource.impl;

import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-url}")
    private String uploadBaseUrl;

    @Override
    public StorageType getStorageType() {
        return StorageType.LOCAL;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        String key = params.getKey();
        String uploadUrl = uploadBaseUrl + "/upload/local?key=" + key;
        return new PresignedUploadUrlResultBO(uploadUrl, key);
    }

    @Override
    public String getFileUrl(Long fileId) {
        return "";
    }

    @Override
    public Map<Long, String> getFileUrls(List<Long> fileIds) {
        return Map.of();
    }
}
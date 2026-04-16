package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.entity.Resource;
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
    public PresignedUploadBO generatePresignedUpload(Resource resource) {
        String key = resource.getStorageKey();
        String uploadUrl = uploadBaseUrl + "/upload/local?key=" + key;
        return new PresignedUploadBO(uploadUrl, key);
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
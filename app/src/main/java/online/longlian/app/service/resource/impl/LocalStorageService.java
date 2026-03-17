package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.entity.FileStorage;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-url}")
    private String uploadBaseUrl;

    @Override
    public PresignedUploadBO generatePresignedUpload(FileStorage fileStorage) {
        String key = fileStorage.getStorageKey();
        String uploadUrl = uploadBaseUrl + "/upload/local?key=" + key;
        return new PresignedUploadBO(uploadUrl, key);
    }
}
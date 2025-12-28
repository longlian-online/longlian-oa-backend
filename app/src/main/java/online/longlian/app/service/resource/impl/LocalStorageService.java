package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.vo.PresignedUpload;
import online.longlian.app.service.resource.StorageService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LocalStorageService implements StorageService {

    @Value("${storage.local.upload-url}")
    private String uploadBaseUrl;

    @Override
    public PresignedUpload generatePresignedUpload(Resource resource) {

        //获取资源的存储 key
        String key = resource.getKey();
        //构造上传 URL
        String uploadUrl = uploadBaseUrl + "/upload/local?key=" + key;

        return new PresignedUpload(uploadUrl, key);
    }
}

package online.longlian.app.service.resource.impl;

import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.app.service.resource.StorageService;
import online.longlian.common.enumeration.StorageType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class NullStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(NullStorageService.class);

    @Override
    public StorageType getStorageType() {
        return StorageType.NONE;
    }

    @Override
    public PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params) {
        log.warn("空存储实现生效，未配置文件存储服务");
        return new PresignedUploadUrlResultBO("", params.getKey());
    }

    @Override
    public String getResourceReadUrl(String key) {
        log.warn("空存储实现生效，未配置文件存储服务");
        return "";
    }

    @Override
    public Map<String, String> getResourceReadUrls(List<String> keys) {
        log.warn("空存储实现生效，未配置文件存储服务");
        return Collections.emptyMap();
    }
}
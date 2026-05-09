package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.common.enumeration.StorageType;

import java.util.List;
import java.util.Map;

public interface StorageService {

    StorageType getStorageType();
    PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params);
    String getResourceReadUrl(String key);

    /**
     * 返回文件地址
     * @param keys 文件 key 列表
     * @return key: key, value: url
     */
    Map<String, String> getResourceReadUrls(List<String> keys);
}
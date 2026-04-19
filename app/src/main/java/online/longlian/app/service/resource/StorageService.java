package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;
import online.longlian.generator.enumeration.StorageType;

import java.util.List;
import java.util.Map;

public interface StorageService {

    StorageType getStorageType();
    PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params);
    String getResourceReadUrl(Long fileId );

    /**
     * 返回文件地址
     * @param fileIds 文件 ID 列表
     * @return key: id, value: url
     */
    Map<Long, String> getResourceReadUrls(List<Long> fileIds);
}
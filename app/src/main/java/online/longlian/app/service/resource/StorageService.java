package online.longlian.app.service.resource;

import online.longlian.app.common.enumeration.StorageType;
import online.longlian.app.pojo.bo.PresignedUploadUrlParamsBO;
import online.longlian.app.pojo.bo.PresignedUploadUrlResultBO;

import java.util.List;
import java.util.Map;

public interface StorageService {

    StorageType getStorageType();
    PresignedUploadUrlResultBO generatePresignedUploadUrl(PresignedUploadUrlParamsBO params);
    String getFileUrl(Long fileId );

    /**
     * 返回文件地址
     * @param fileIds 文件id
     * @return key: id, value: url
     */
    Map<Long, String> getFileUrls(List<Long> fileIds);
}
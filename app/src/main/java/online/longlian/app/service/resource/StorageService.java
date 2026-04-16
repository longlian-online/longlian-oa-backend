package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.entity.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public interface StorageService {

    PresignedUploadBO generatePresignedUpload(Resource fileStorage);
    String getFileUrl(Long fileId );

    /**
     * 返回文件地址
     * @param fileIds 文件id
     * @return key: id, value: url
     */
    Map<Long, String> getFileUrls(List<Long> fileIds);
}
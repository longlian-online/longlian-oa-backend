package online.longlian.app.service.resource;

import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.bo.PresignedUpload;

public interface StorageService {

    /**
     * 生成预签名上传信息
     */
    PresignedUpload generatePresignedUpload(Resource resource);
}

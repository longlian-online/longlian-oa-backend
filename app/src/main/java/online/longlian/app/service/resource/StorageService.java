package online.longlian.app.service.resource;

import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.pojo.dto.PresignedUploadDTO;

public interface StorageService {

    /**
     * 生成预签名上传信息
     */
    PresignedUploadDTO generatePresignedUpload(Resource resource);
}

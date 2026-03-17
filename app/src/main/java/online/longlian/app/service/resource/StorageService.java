package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.PresignedUploadBO;
import online.longlian.app.pojo.entity.FileStorage;

public interface StorageService {

    PresignedUploadBO generatePresignedUpload(FileStorage fileStorage);

}
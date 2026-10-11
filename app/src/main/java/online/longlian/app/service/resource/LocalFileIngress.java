package online.longlian.app.service.resource;

import lombok.RequiredArgsConstructor;
import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;
import online.longlian.app.pojo.bo.common.LocalFileReadParamsBO;
import online.longlian.app.pojo.bo.common.LocalFileWriteParamsBO;
import online.longlian.app.pojo.entity.Resource;
import online.longlian.app.service.resource.impl.LocalStorageService;
import online.longlian.common.enumeration.StorageType;
import org.springframework.stereotype.Service;

import java.io.InputStream;

@Service
@RequiredArgsConstructor
public class LocalFileIngress {

    private final ResourceService resourceService;
    private final LocalStorageService localStorageService;
    private final LocalFileUrlSigner signer;

    public void upload(LocalFileReadParamsBO signed, InputStream content, long contentLength) {
        signer.verifyUpload(signed);
        Resource pending = requireLocal(resourceService.loadPending(signed.key()),
                ResultCode.UNAUTHORIZED_OPERATION, "无权上传或文件已完成上传");
        if (contentLength >= 0 && !pending.getFileSize().equals(contentLength)) {
            throw new AppException(ResultCode.PARAM_ERROR, "文件大小不匹配");
        }
        localStorageService.upload(LocalFileWriteParamsBO.builder()
                .storageKey(signed.key())
                .content(content)
                .expectedSize(pending.getFileSize())
                .expectedMimeType(pending.getFileMime())
                .build());
    }

    public org.springframework.core.io.Resource read(LocalFileReadParamsBO signed) {
        signer.verify(signed);
        requireLocal(resourceService.loadActivated(signed.key()), ResultCode.DATA_NOT_EXIT);
        return localStorageService.getResource(signed.key());
    }

    private static Resource requireLocal(Resource resource, ResultCode code) {
        if (resource.getStorageType() != StorageType.LOCAL) {
            throw new AppException(code);
        }
        return resource;
    }

    private static Resource requireLocal(Resource resource, ResultCode code, String message) {
        if (resource.getStorageType() != StorageType.LOCAL) {
            throw new AppException(code, message);
        }
        return resource;
    }

}

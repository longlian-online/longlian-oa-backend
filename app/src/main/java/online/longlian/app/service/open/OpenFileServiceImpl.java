package online.longlian.app.service.open;

import lombok.RequiredArgsConstructor;
import online.longlian.app.pojo.bo.common.ResourceCreateParamsBO;
import online.longlian.app.pojo.bo.common.ResourceCreateResultBO;
import online.longlian.app.pojo.bo.open.OpenFileCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenFileCreateResultBO;
import online.longlian.app.service.resource.ResourceService;
import org.springframework.stereotype.Service;

/**
 * 开放面上传资源创建（/open/v1/files）。
 * 复用站内 ResourceService 的全部大小与类型校验、storageKey 生成与预签名链接签发（standard #7/#10），
 * 本域只做开放面口径：业务类型固定 task_submit，出参按站内 ResourceCreateVO 收窄、不外泄 storageKey。
 */
@Service
@RequiredArgsConstructor
public class OpenFileServiceImpl implements OpenFileService {

    /** 开放面上传只服务任务提交，业务类型不随请求变化。 */
    private static final String UPLOAD_BIZ_TYPE = "task_submit";

    private final ResourceService resourceService;

    @Override
    public OpenFileCreateResultBO createUpload(OpenFileCreateParamsBO params) {
        ResourceCreateResultBO result = resourceService.create(new ResourceCreateParamsBO(
                params.userId(), params.orgId(), params.fileName(), params.fileExt(),
                params.fileSize(), params.fileMime(), UPLOAD_BIZ_TYPE));
        return new OpenFileCreateResultBO(result.getFileId(), result.getUploadUrl(),
                result.getStorageType(), result.getContentDisposition());
    }
}

package online.longlian.app.service.open;

import online.longlian.app.pojo.bo.open.OpenFileCreateParamsBO;
import online.longlian.app.pojo.bo.open.OpenFileCreateResultBO;

/**
 * 开放面上传资源创建。大小与类型校验、预签名链接生成全部复用站内 ResourceService。
 */
public interface OpenFileService {

    /**
     * 创建 task_submit 业务类型的上传资源，返回预签名直传地址等信息。
     */
    OpenFileCreateResultBO createUpload(OpenFileCreateParamsBO params);
}

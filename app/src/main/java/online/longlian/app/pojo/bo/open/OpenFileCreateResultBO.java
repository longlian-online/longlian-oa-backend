package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.StorageType;

/**
 * 开放面创建上传资源出参：按站内 ResourceCreateVO 收窄，不含 storageKey。
 * 该结果会整体进入幂等快照，字段必须小且稳定。
 */
public record OpenFileCreateResultBO(Long fileId, String uploadUrl,
                                     StorageType storageType, String contentDisposition) {
}

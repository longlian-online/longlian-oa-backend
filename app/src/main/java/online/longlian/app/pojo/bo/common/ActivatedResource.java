package online.longlian.app.pojo.bo.common;

import online.longlian.common.enumeration.StorageType;

/** 已激活且匹配到某个业务对象的资源，不含签名读取地址。 */
public record ActivatedResource(
        Long id,
        String fileName,
        Long fileSize,
        String fileMime,
        StorageType storageType,
        String storageKey,
        Long orgId) {
}

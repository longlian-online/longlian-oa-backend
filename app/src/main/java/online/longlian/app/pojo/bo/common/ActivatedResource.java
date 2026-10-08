package online.longlian.app.pojo.bo.common;

import online.longlian.common.enumeration.StorageType;

/** An activated resource matched to one business object, without a signed read URL. */
public record ActivatedResource(
        Long id,
        String fileName,
        Long fileSize,
        String fileMime,
        StorageType storageType,
        String storageKey,
        Long orgId) {
}

package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.common.enumeration.StorageType;

@Data
@AllArgsConstructor
public class ResourceCreateResultBO {

    private Long fileId;

    private String uploadUrl;

    private String key;

    private StorageType storageType;

    private String contentDisposition;
}

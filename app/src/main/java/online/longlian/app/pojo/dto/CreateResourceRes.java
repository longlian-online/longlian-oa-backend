package online.longlian.app.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.enumeration.ResourceStorageType;

@Data
@AllArgsConstructor
public class CreateResourceRes {

    private String resourceId;
    private String uploadUrl;
    private String key;
    private ResourceStorageType storageType;
}

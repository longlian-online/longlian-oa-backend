package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.enumeration.StorageType;

@Data
@AllArgsConstructor
public class ResourceCreateParamsBO {
    private Long creatorId;
    private Long orgId;
    private StorageType storageType;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String fileMime;
    private String bizType;
    private Long bizId;
}

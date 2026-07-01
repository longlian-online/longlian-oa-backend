package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ResourceCreateParamsBO {
    private Long creatorId;
    private Long orgId;
    private String fileName;
    private String fileExt;
    private Long fileSize;
    private String fileMime;
    private String bizType;
    private Long bizId;
}

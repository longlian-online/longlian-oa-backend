package online.longlian.app.pojo.bo.app;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TaskAttachmentDownloadParamsBO {
    private Long instanceId;
    private Long fileId;
    private Long orgId;
    private Long userId;
}

package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAttachmentDeleteParamsBO {
    private Long projectId;
    private Long attachmentId;
    private Long userId;
    private Long orgId;
}

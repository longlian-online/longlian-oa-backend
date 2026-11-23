package online.longlian.app.pojo.bo.app.project;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectAttachmentDownloadParamsBO {
    private Long projectId;
    private Long attachmentId;
    private Long orgId;
}

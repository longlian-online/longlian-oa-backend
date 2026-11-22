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
    /** 开放面按文件 ID 解析附件行；站内仍用 attachmentId */
    private Long fileId;
    private Long orgId;
}

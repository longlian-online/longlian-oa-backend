package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgFileBatchFileResultBO {
    private Long taskInstanceId;
    private String fieldKey;
    private Long fileId;
    private String fileName;
    private Long fileSize;
    private String typeName;
    private String projectTitle;
    private String itemTitle;
    private String taskName;
    private String fieldLabel;
    private Integer versionNo;
}

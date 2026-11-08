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
    private String readUrl;
    private Long expireAt;
}

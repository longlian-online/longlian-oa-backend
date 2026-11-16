package online.longlian.app.pojo.bo.orgadmin.file;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgFileDownloadParamsBO {
    private Long orgId;
    private Long taskInstanceId;
    private Long fileId;
}

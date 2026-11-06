package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgFileDiscardParamsBO {
    private Long orgId;
    private Long taskInstanceId;
    private Long fileId;
    private String fieldKey;
}

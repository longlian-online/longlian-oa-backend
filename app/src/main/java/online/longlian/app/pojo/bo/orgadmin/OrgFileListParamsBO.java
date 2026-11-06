package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgFileListParamsBO {
    private Long orgId;
    private Long projectId;
    private Long itemId;
    private Integer pageNum;
    private Integer pageSize;
}

package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.common.enumeration.OrgFileView;

@Data
@Builder
public class OrgFileListParamsBO {
    private Long orgId;
    private OrgFileView view;
    private Long typeId;
    private Long projectId;
    private Long itemId;
    private Integer pageNum;
    private Integer pageSize;
}

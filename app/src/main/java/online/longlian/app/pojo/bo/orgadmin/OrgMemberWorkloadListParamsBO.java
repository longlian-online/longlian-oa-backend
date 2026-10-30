package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
public class OrgMemberWorkloadListParamsBO {
    private Long orgId;
    private String keyword;
    private SortDirection orderDir;
    private PageParamsBO page;
}

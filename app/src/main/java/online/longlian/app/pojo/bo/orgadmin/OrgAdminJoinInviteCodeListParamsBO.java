package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;
import online.longlian.app.pojo.bo.common.PageParamsBO;

@Data
@Builder
public class OrgAdminJoinInviteCodeListParamsBO {
    private Long orgId;
    private String status;
    private PageParamsBO page;
}

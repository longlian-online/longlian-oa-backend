package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgMemberClaimedTaskParamsBO {
    private Long memberId;
    private Long orgId;
}

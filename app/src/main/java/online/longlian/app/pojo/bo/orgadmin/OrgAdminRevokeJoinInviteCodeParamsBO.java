package online.longlian.app.pojo.bo.orgadmin;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgAdminRevokeJoinInviteCodeParamsBO {
    private Long orgId;
    private Long inviteId;
}

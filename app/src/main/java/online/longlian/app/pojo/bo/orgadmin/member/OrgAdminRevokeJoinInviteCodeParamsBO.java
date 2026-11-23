package online.longlian.app.pojo.bo.orgadmin.member;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrgAdminRevokeJoinInviteCodeParamsBO {
    private Long orgId;
    private Long inviteId;
}

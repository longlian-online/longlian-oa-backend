package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrgAdminGenerateJoinOrgInviteCodeParamsBO {
    private Long creatorId;
    private Long orgId;
}
